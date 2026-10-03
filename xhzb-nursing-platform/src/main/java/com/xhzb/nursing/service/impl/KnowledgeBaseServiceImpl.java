package com.xhzb.nursing.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.xhzb.nursing.mapper.KnowledgeBaseMapper;
import com.xhzb.nursing.service.IKnowledgeBaseService;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 知识库Service业务层处理
 * 
 * @author gaozf
 * @date 2026-10-01
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService
{
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private OSSAliyunFileStorageService fileStorageService;

    @Autowired
    private TextSplitter textSplitter;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KnowledgeBase selectKnowledgeBaseById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase)
    {
        return knowledgeBaseMapper.selectKnowledgeBaseList(knowledgeBase);
    }

    /**
     * 新增知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        //1,下载阿里云oss文件
        InputStream inputStream = fileStorageService.download(knowledgeBase.getDocumentUrl());
        if (inputStream == null) {
            throw new BaseException("文档不存在");
        }
        //2，按页读取pdf文档
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(inputStream),
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());
        List<Document> documentList = pdfReader.read();
        //3，用textspilier切割
        List<Document> newDocumentList = textSplitter.apply(documentList);

        //4，将切割的document文档对象集合，分批保存到redis向量库
        int batchSize = 10;
        for (int i =0 ;i<newDocumentList.size();i+=batchSize){
            List<Document> timList = newDocumentList.subList(i, Math.min(i + batchSize, newDocumentList.size()));
            vectorStore.add(timList);
            System.out.println("批次："+(i+batchSize)/10+","+"保存了"+timList.size()+"个文档");
        }
        //5，存知识数据库到mysql
        List<String> docIdList = newDocumentList.stream().map(Document::getId).toList(); //切割后的文档id集合
        //存到知识库的remark字段
        knowledgeBase.setRemark(JSONUtil.toJsonStr(docIdList)); //["xxxx","xxxxx"]
        return save(knowledgeBase)? 1 : 0;
    }

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        return updateById(knowledgeBase)? 1 : 0;
    }

    /**
     * 批量删除知识库
     * 
     * @param ids 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除知识库信息
     * 
     * @param id 知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseById(Long id)
    {
        //1,根据id查询mysql中知识库数据 做校验
        //KnowledgeBase knowledgeBase = getById(id); //MP提供的方法
        KnowledgeBase knowledgeBase = selectKnowledgeBaseById(id);
        if (knowledgeBase == null) {
            throw new BaseException("知识库不存在");
        }
        //2，删除redis中向量数据
        String remark = knowledgeBase.getRemark();
        List<String> docIdList = JSONUtil.toList(remark, String.class);
        vectorStore.delete(docIdList);
        //3，删除阿里云oss中文件
        String documentUrl = knowledgeBase.getDocumentUrl();
        fileStorageService.delete(documentUrl);
        //4，删除mysql数据
        return removeById(id)? 1 : 0;
    }
}
