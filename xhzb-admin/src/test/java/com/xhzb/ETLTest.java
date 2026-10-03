package com.xhzb;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@SpringBootTest
public class ETLTest {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 读取txt文本数据
     */
    @Test
    void testReader1() throws FileNotFoundException {

        InputStreamResource resource = new InputStreamResource(new InputStreamResource(new FileInputStream("/Users/gao/Desktop/文件夹管理-三级文件关联接口变更.txt")));
        //String resourceUrl = "/Users/gao/Desktop/文件夹管理-三级文件关联接口变更.txt";
        TextReader reader = new TextReader(resource); //参数如果是字符串，不能写绝对路径，其实从resourse目录读取文件
        reader.getCustomMetadata().put("fileName","文档修改");//修改元数据
        List<Document> documentList = reader.read();
        for (Document document : documentList) {
            System.out.println(document);
        }
    }
    /**
     * 按页数读取pdf
     */
    @Test
    void testReader2() throws FileNotFoundException {
        InputStreamResource resource = new InputStreamResource(new FileInputStream("/Users/gao/Desktop/文件夹管理-三级文件关联接口变更.txt"));
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());
        List<Document> documentList = pdfReader.read();
        for (Document document : documentList) {
            System.out.println(document);
        }
    }

    /**
     * 按段落读取pdf
     */
    @Test
    void testReader3() throws FileNotFoundException {
        InputStreamResource resource = new InputStreamResource(new FileInputStream("/Users/gao/Desktop/文件夹管理-三级文件关联接口变更.txt"));
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());

        List<Document> documentList = pdfReader.read();
        for (Document document : documentList) {
            System.out.println(document);
        }
    }

    /**
     * 按页面读取pdf采用分割textspiliter文档切割器
     * @throws FileNotFoundException
     */
    @Test
    void testReader4() throws FileNotFoundException {
        InputStreamResource resource = new InputStreamResource(new FileInputStream("/Users/gao/Desktop/护理员工工作手册.pdf"));
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());
        List<Document> documentList = pdfReader.read();
        System.out.println(documentList.size());

        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> newDocumentList = textSplitter.apply(documentList);//31个

        //10个10个保存
        for (int i = 0; i < newDocumentList.size(); i+=10) {
            //每次从documentList截取十个保存
            //第一次运行 i=0 0-10
            //第二次运行 i=10 10-20
            //第三次运行 i=20 20-30
            //第四次运行 i=30 30-31
            List<Document> subDocList = newDocumentList.subList(i, Math.min(i + 10, newDocumentList.size()));
            vectorStore.add(subDocList); //将切割后的document保存到redis向量数据库
        }
    }

    @Test
    void testRetrival(){
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore) //使用向量数据库
                .similarityThreshold(0.73) //相似度域值
                .topK(5)//前几个
//                .filterExpression(new FilterExpressionBuilder() //过滤使用元数据
//                        .eq("genre", "fairytale")
//                        .build())
                .build();
        List<Document> documents = retriever.retrieve(new Query("护理服务宗旨与核心价值是什么"));
        for (Document document : documents) {
            System.out.println(document);
        }
    }
}
