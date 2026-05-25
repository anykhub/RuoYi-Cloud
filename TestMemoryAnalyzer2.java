import java.util.ArrayList;
import java.util.List;

public class TestMemoryAnalyzer2 {
    public static class ExampleDTO {
        public Long id = 1L;
        public String username = "test_user_name_string_for_memory";
        public Integer age = 25;
        // Padding to simulate real world DTO size
        public String field1 = "AAAAAAAAAAAAAAAAAAAA";
        public String field2 = "BBBBBBBBBBBBBBBBBBBB";
        public String field3 = "CCCCCCCCCCCCCCCCCCCC";

        public ExampleDTO() {
            // allocate new strings to prevent interning optimization
            username = new String("test_user_name_string_for_memory");
            field1 = new String("AAAAAAAAAAAAAAAAAAAA");
            field2 = new String("BBBBBBBBBBBBBBBBBBBB");
            field3 = new String("CCCCCCCCCCCCCCCCCCCC");
        }
    }

    public static void main(String[] args) throws Exception {
        int count = 1000000; // 百万数据
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();

        List<ExampleDTO> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new ExampleDTO());
        }

        System.gc();
        long after = rt.totalMemory() - rt.freeMemory();
        System.out.println("百万数据 List<T> 占用内存: " + (after - before) / 1024 / 1024 + " MB");
        // Keep reference so it doesn't get GC'd
        System.out.println(list.get(500000).username);
    }
}
