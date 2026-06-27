import os
import zipfile
import xml.etree.ElementTree as ET
import csv
import random
import re
import xlsxwriter

# Real country list (in Chinese)
REAL_COUNTRIES = [
    "中国", "美国", "英国", "法国", "德国", "日本", "韩国", "加拿大", "澳大利亚", "俄罗斯",
    "巴西", "印度", "新加坡", "意大利", "西班牙", "荷兰", "瑞士", "瑞典", "新西兰", "南非",
    "墨西哥", "越南", "泰国", "马来西亚", "菲律宾", "印度尼西亚", "阿联酋", "沙特阿拉伯", "土耳其", "埃及",
    "阿根廷", "智利", "哥伦比亚", "秘鲁", "爱尔兰", "比利时", "奥地利", "丹麦", "挪威", "芬兰",
    "波兰", "希腊", "葡萄牙", "捷克", "匈牙利", "罗马尼亚", "哈萨克斯坦", "以色列", "巴基斯坦", "孟加拉国"
]

def is_numeric(val):
    """Check if the value is an integer or float with a .0 suffix (common in Excel parsing)."""
    if val is None:
        return False
    val_str = str(val).strip()
    return bool(re.match(r'^\d+(\.0+)?$', val_str))

def parse_int_robust(val):
    """Parse a numeric string to integer robustly by removing any .0 decimal suffix."""
    val_str = str(val).strip()
    if '.' in val_str:
        val_str = val_str.split('.')[0]
    return int(val_str)

def get_column_index(cell_ref):
    """Convert cell reference (e.g. 'A1', 'B2', 'AA5') to 0-based column index."""
    col_str = ""
    for char in cell_ref:
        if char.isalpha():
            col_str += char
        else:
            break
    
    exp = 0
    col_idx = 0
    for char in reversed(col_str):
        col_idx += (ord(char.upper()) - 64) * (26 ** exp)
        exp += 1
    return col_idx - 1

def read_first_row_from_xlsx(file_path):
    """
    Read headers and the first data row from the XLSX file without external dependencies.
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    with zipfile.ZipFile(file_path, 'r') as z:
        # 1. Load shared strings
        shared_strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            ss_data = z.read('xl/sharedStrings.xml')
            root = ET.fromstring(ss_data)
            ns = {'ns': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
            for si in root.findall('ns:si', ns):
                t_nodes = si.findall('.//ns:t', ns)
                val = "".join([t.text for t in t_nodes if t.text is not None])
                shared_strings.append(val)
        
        # 2. Load worksheet sheet1.xml
        sheet_data = z.read('xl/worksheets/sheet1.xml')
        root = ET.fromstring(sheet_data)
        ns = {'ns': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
        
        # We need headers (row 1) and the first data row (row 2)
        headers = {}
        data_row = {}
        
        for r in root.findall('.//ns:row', ns):
            row_num = int(r.attrib.get('r', 0))
            if row_num not in (1, 2):
                continue
                
            for c in r.findall('ns:c', ns):
                cell_ref = c.attrib.get('r', '')
                if not cell_ref:
                    continue
                col_idx = get_column_index(cell_ref)
                
                v_node = c.find('ns:v', ns)
                val = ""
                if v_node is not None and v_node.text is not None:
                    val = v_node.text
                    t_attr = c.attrib.get('t')
                    if t_attr == 's':  # shared string
                        idx = int(val)
                        val = shared_strings[idx] if idx < len(shared_strings) else ""
                
                if row_num == 1:
                    headers[col_idx] = val
                elif row_num == 2:
                    data_row[col_idx] = val
        
        # Build sorted list based on column indices
        all_cols = sorted(list(set(headers.keys()) | set(data_row.keys())))
        header_list = [headers.get(col, "") for col in all_cols]
        data_list = [data_row.get(col, "") for col in all_cols]
        
        return header_list, data_list

def generate_value(col_name, seed_val, global_row_idx, field_group_sizes, country_cache):
    """
    核心字段生成逻辑：根据字段配置的分组大小计算出对应行的值
    """
    group_size = field_group_sizes.get(col_name)
    if group_size is None:
        return seed_val
        
    group_index = global_row_idx // group_size
    
    if col_name == 'country':
        if group_index not in country_cache:
            country_cache[group_index] = random.choice(REAL_COUNTRIES)
        return country_cache[group_index]
        
    if is_numeric(seed_val):
        base_num = parse_int_robust(seed_val)
        return str(base_num + group_index)
    else:
        if group_size == 1:
            return f"{seed_val}_{global_row_idx + 1}"
        else:
            suffix = str(group_index) if group_index > 0 else ""
            return f"{seed_val}{suffix}"

def generate_million_data(input_xlsx, output_csv, target_size_mb=100, field_group_sizes=None):
    """
    Generate million-level data to a CSV file.
    """
    if field_group_sizes is None:
        field_group_sizes = {
            "bdnm": 10,
            "bdfh": 10,
            "country": 2000
        }
    else:
        field_group_sizes = {k.lower(): v for k, v in field_group_sizes.items()}

    print("Reading seed data from Excel...")
    headers, first_row = read_first_row_from_xlsx(input_xlsx)
    print(f"Headers: {headers}")
    print(f"Seed Row: {first_row}")
    
    # Estimate total rows
    total_expected = estimate_total_rows_csv(input_xlsx, target_size_mb, field_group_sizes)
    print(f"Generating {total_expected} rows into CSV {output_csv}...")
    
    random.seed(42)
    country_cache = {}
    
    with open(output_csv, 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        
        batch_size = 10000
        batch = []
        
        for global_row_idx in range(total_expected):
            row_data = []
            for idx, h in enumerate(headers):
                h_lower = h.strip().lower()
                seed_val = first_row[idx] if idx < len(first_row) else ""
                val = generate_value(h_lower, seed_val, global_row_idx, field_group_sizes, country_cache)
                row_data.append(val)
            
            batch.append(row_data)
            
            if len(batch) >= batch_size:
                writer.writerows(batch)
                batch = []
                    
        if batch:
            writer.writerows(batch)
            
    print(f"Done! Successfully generated {total_expected} rows.")

def generate_million_data_xlsx(input_xlsx, output_xlsx, target_size_mb=100, field_group_sizes=None, max_rows_per_sheet=1000000):
    """
    Generate million-level data to an Excel file with multiple sheets support.
    """
    if field_group_sizes is None:
        field_group_sizes = {
            "bdnm": 10,
            "bdfh": 10,
            "存量1": 10,
            "country": 2000
        }
    else:
        field_group_sizes = {k.lower(): v for k, v in field_group_sizes.items()}

    print("Reading seed data from Excel...")
    headers, first_row = read_first_row_from_xlsx(input_xlsx)
    print(f"Headers: {headers}")
    print(f"Seed Row: {first_row}")
    
    # Estimate total rows
    total_expected = estimate_total_rows_xlsx(input_xlsx, target_size_mb, field_group_sizes)
    print(f"Generating {total_expected} rows into Excel file {output_xlsx}...")
    
    random.seed(42)
    country_cache = {}
    
    workbook = xlsxwriter.Workbook(output_xlsx, {'constant_memory': True})
    
    sheet_count = 0
    worksheet = None
    row_in_sheet = 0
    
    for global_row_idx in range(total_expected):
        if worksheet is None or row_in_sheet > max_rows_per_sheet:
            sheet_count += 1
            sheet_name = f"Sheet{sheet_count}"
            print(f"Creating new sheet: {sheet_name}")
            worksheet = workbook.add_worksheet(sheet_name)
            # Write headers dynamically
            for col_idx, h in enumerate(headers):
                worksheet.write(0, col_idx, h)
            row_in_sheet = 1
            
        for col_idx, h in enumerate(headers):
            h_lower = h.strip().lower()
            seed_val = first_row[col_idx] if col_idx < len(first_row) else ""
            val = generate_value(h_lower, seed_val, global_row_idx, field_group_sizes, country_cache)
            worksheet.write(row_in_sheet, col_idx, val)
            
        row_in_sheet += 1
            
    workbook.close()
    print(f"Done! Successfully generated Excel workbook with {sheet_count} sheets.")

def estimate_total_rows_csv(input_xlsx, target_size_mb=100, field_group_sizes=None, pilot_rows=5000):
    import io
    if field_group_sizes is None:
        field_group_sizes = {
            "bdnm": 10,
            "bdfh": 10,
            "country": 2000
        }
    else:
        field_group_sizes = {k.lower(): v for k, v in field_group_sizes.items()}

    headers, first_row = read_first_row_from_xlsx(input_xlsx)
    
    mem_file = io.StringIO()
    writer = csv.writer(mem_file)
    writer.writerow(headers)
    
    country_cache = {}
    random.seed(42)
    
    for r in range(1, pilot_rows + 1):
        global_row_idx = r - 1
        row_data = []
        for idx, h in enumerate(headers):
            h_lower = h.strip().lower()
            seed_val = first_row[idx] if idx < len(first_row) else ""
            val = generate_value(h_lower, seed_val, global_row_idx, field_group_sizes, country_cache)
            row_data.append(val)
        writer.writerow(row_data)
        
    mem_file.seek(0)
    content_bytes = mem_file.getvalue().encode('utf-8-sig')
    size = len(content_bytes)
    bytes_per_row = size / pilot_rows
    target_bytes = target_size_mb * 1024 * 1024
    total_rows = int(round(target_bytes / bytes_per_row))
    
    print(f"CSV Pilot Run: size for {pilot_rows} rows is {size/1024:.2f} KB. Bytes per row: {bytes_per_row:.4f}. Target rows: {total_rows}")
    return total_rows

def estimate_total_rows_xlsx(input_xlsx, target_size_mb=100, field_group_sizes=None, pilot_rows=5000):
    import io
    if field_group_sizes is None:
        field_group_sizes = {
            "bdnm": 10,
            "bdfh": 10,
            "存量1": 10,
            "country": 2000
        }
    else:
        field_group_sizes = {k.lower(): v for k, v in field_group_sizes.items()}

    headers, first_row = read_first_row_from_xlsx(input_xlsx)
    
    mem_file = io.BytesIO()
    workbook = xlsxwriter.Workbook(mem_file, {'constant_memory': True})
    worksheet = workbook.add_worksheet("Pilot")
    
    for col_idx, h in enumerate(headers):
        worksheet.write(0, col_idx, h)
        
    country_cache = {}
    random.seed(42)
    
    for r in range(1, pilot_rows + 1):
        global_row_idx = r - 1
        for col_idx, h in enumerate(headers):
            h_lower = h.strip().lower()
            seed_val = first_row[col_idx] if col_idx < len(first_row) else ""
            val = generate_value(h_lower, seed_val, global_row_idx, field_group_sizes, country_cache)
            worksheet.write(r, col_idx, val)
            
    workbook.close()
    size = mem_file.tell()
    bytes_per_row = size / pilot_rows
    target_bytes = target_size_mb * 1024 * 1024
    total_rows = int(round(target_bytes / bytes_per_row))
    
    print(f"Excel Pilot Run: compressed size for {pilot_rows} rows is {size/1024:.2f} KB. Bytes per row: {bytes_per_row:.4f}. Target rows: {total_rows}")
    return total_rows

if __name__ == '__main__':
    # Settings:
    # 目标生成物理大小 (单位: MB)
    target_size_mb = 100
    
    input_file = r'd:\fengyong\RuoYi-Cloud-3.6.4\bd.xlsx'
    output_file_csv = r'd:\fengyong\RuoYi-Cloud-3.6.4\bd_million_100m.csv'
    output_file_xlsx = r'd:\fengyong\RuoYi-Cloud-3.6.4\bd_million_100m.xlsx'
    
    # CSV 的字段分组大小配置
    csv_field_group_sizes = {
        "bdnm": 10,
        "bdfh": 10,
        "country": 2000
    }
    
    # Excel 的字段分组大小配置
    xlsx_field_group_sizes = {
        "bdnm": 10,
        "bdfh": 10,
        "存量1": 10,
        "country": 2000
    }
    
    # 1. 动态生成指定大小的 CSV
    generate_million_data(
        input_xlsx=input_file,
        output_csv=output_file_csv,
        target_size_mb=target_size_mb,
        field_group_sizes=csv_field_group_sizes
    )
    
    # 2. 动态生成指定大小的 Excel
    generate_million_data_xlsx(
        input_xlsx=input_file,
        output_xlsx=output_file_xlsx,
        target_size_mb=target_size_mb,
        field_group_sizes=xlsx_field_group_sizes,
        max_rows_per_sheet=1000000
    )
