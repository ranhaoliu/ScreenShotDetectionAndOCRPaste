package com.aliyun.orc;

// This file is auto-generated, don't edit it. Thanks.
import android.Manifest;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
//import com.aliyun.sample.pojo.JsonRootBean;
import com.aliyun.tea.*;
import com.aliyun.ocr_api20210707.*;
import com.aliyun.ocr_api20210707.models.*;
import com.aliyun.teaopenapi.*;
import com.aliyun.teaopenapi.models.*;
import com.aliyun.teautil.*;
import com.aliyun.teautil.models.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Sample {

    /**
     * 使用AK&SK初始化账号Client
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.ocr_api20210707.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        Config config = new Config()
                // 您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "ocr-api.cn-hangzhou.aliyuncs.com";
        return new com.aliyun.ocr_api20210707.Client(config);
    }

    /**
     * 调用阿里云OCR接口进行文字识别
     */
    public static String takeImageToText(InputStream inputStream) throws Exception {
        com.aliyun.ocr_api20210707.Client client = Sample.createClient("yourAccessKeyId", "yourAccessKeySecret");
        RecognizeGeneralRequest recognizeGeneralRequest = new RecognizeGeneralRequest();
        String content="";
        recognizeGeneralRequest.setBody(inputStream);
        RuntimeOptions runtime = new RuntimeOptions();
        try {
            // 复制代码运行请自行打印 API 的返回值
            RecognizeGeneralResponse response =client.recognizeGeneralWithOptions(recognizeGeneralRequest, runtime);
            RecognizeGeneralResponseBody body= response.getBody();
            Integer code= response.getStatusCode();
            //将json字符串转json对象，Feature.OrderedField防止乱序
            JSONObject jsonObject = JSON.parseObject(body.data, Feature.OrderedField);
            content = jsonObject.get("content").toString();
        } catch (TeaException error) {
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            Log.v("-----", error.message);
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        }
        return content;
    }

}

