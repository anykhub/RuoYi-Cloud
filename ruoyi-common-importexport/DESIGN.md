# 总体架构

通用导入导出组件采用高度解耦、职责单一的架构设计。通过一系列设计模式的应用，将不同文件类型（Excel、CSV、JSON、XML、TXT）的导入导出逻辑进行了统一的抽象与封装，使得整个组件能够以极简的 API 供外部调用，并具备极强的横向扩展能力。

组件核心划分为以下几层：
1. **统一门面层 (Factory + Registry)**：提供对外统一的方法入口，根据业务传入的文件类型，动态分发到对应的处理策略。
2. **核心抽象层 (Template Method)**：抽象出 `ImportExportHandler` 接口和 `AbstractImportExportHandler` 抽象类，规范了所有导入导出的生命周期（读、写、校验），实现基本骨架。
3. **具体实现层 (Strategy)**：不同文件类型具有各自独立的处理实现，如 `ExcelHandler` 负责基于 EasyExcel 流式处理数据，`CsvHandler` 负责基于 Apache Commons CSV 读写数据等。
4. **扩展能力层**：设计了统一的异常处理 (`ImportExportException`) 与结果反馈 (`ImportResult`)，支持业务端的校验错误收集。

# 类设计

- **`ImportExportHandler<T>`**: 核心顶层接口，定义了 `exportData` 和 `importData` 的标准契约。
- **`AbstractImportExportHandler<T>`**: 抽象模板类，实现顶层接口，并通过 `InitializingBean` 完成自身在 Spring 容器启动时的自动注册；定义了 `doExport` 和 `doImport` 作为抽象方法交由子类实现。
- **`HandlerRegistry`**: 注册中心，本质是一个维护 `Map<FileTypeEnum, ImportExportHandler>` 的单例 Bean。
- **`FileHandlerFactory`**: 业务暴露的工厂类，屏蔽 Registry 的细节，直接返回具体的 Handler 实例。
- **`FileTypeEnum`**: 文件类型枚举，规范目前支持的扩展名。
- **`ImportResult<T>`**: 导入结果 DTO，封装了成功数据的集合以及失败校验的错误信息集合。
- **具体 Handler (ExcelHandler, CsvHandler 等)**: 负责具体格式的文件输入输出逻辑。

# 设计模式

1. **策略模式 (Strategy)**: 将不同的文件处理逻辑封装成不同的类 (`ExcelHandler`, `CsvHandler` 等)。它们都实现同样的 `ImportExportHandler` 接口，可以在运行时相互替换。这极大地消除了主流程中的 `if-else`。
2. **工厂模式 (Factory)**: 提供了 `FileHandlerFactory`。调用方只需告诉工厂我需要 `FileTypeEnum.EXCEL`，工厂就会返回相应的策略对象，调用方不需要关心这个对象是如何被创建和查找的。
3. **模板方法模式 (Template Method)**: 在 `AbstractImportExportHandler` 中规定了处理的基本骨架（例如校验输入输出对象是否为空、容器注册），并将具体解析流的操作推迟到抽象方法 `doExport` 和 `doImport` 中让子类必须实现。
4. **注册中心模式 (Registry)**: `HandlerRegistry` 作为统一的调度中心。利用 Spring 的 `InitializingBean`，各具体的 Handler 在实例化后会自动向 Registry 注册自己。这样在增加新的文件类型时，只需新增一个 Handler 类，无需修改工厂或注册中心的代码，完全符合开闭原则（OCP）。

# 核心代码

组件已在 `com.ruoyi.common.importexport` 包下完整实现。包含 `pom.xml` 中引入了必要的依赖：EasyExcel、Jackson Dataformat XML、Commons CSV 以及 Spring Boot Validation。

# 示例代码

请参考项目中 `ExampleController` 和 `ExampleService` 的实现：
- 使用统一泛型 `ExampleDTO` 承载具体业务。
- 服务层自动完成数据装配、字段 Validation 校验。
- 控制层通过 REST API 接收文件与抛出流。

# 扩展方式

本组件具有极强的可扩展性，**如果需要新增一种文件类型（例如 PDF 或者 Word 导出）**，只需执行以下两步，**无需修改任何现有代码**：
1. 在 `FileTypeEnum` 中增加一个枚举项。
2. 新建一个类继承 `AbstractImportExportHandler<T>`，实现 `getSupportedFileType()`、`doExport()`、`doImport()` 方法，并在类上加上 `@Component` 注解即可。

# 最佳实践

- **大数据量导出**：对于 Excel 文件，`ExcelHandler` 默认使用了 EasyExcel 的流式写入功能，直接将数据块落盘，不会引起 OOM。同理，CSV/TXT 实现亦使用 `CSVPrinter` / `BufferedWriter` 完成了流式写出。
- **异常捕获**：尽量在 Controller 层统一利用 RuoYi 本身的全局异常处理器拦截 `ImportExportException`。
- **校验集成**：利用 Hibernate Validator，在读取完成对象后集中校验（见 `ExampleService.java`），然后将错误行及原因输出给前端用户。