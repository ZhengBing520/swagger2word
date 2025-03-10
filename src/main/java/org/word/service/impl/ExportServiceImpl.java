package org.word.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.word.model.ApiTplExcelData;
import org.word.model.Table;
import org.word.model.TableInfo;
import org.word.service.ExportService;
import org.word.service.WordService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {
    private final WordService tableService;

    @Autowired
    public ExportServiceImpl(WordService tableService) {
        this.tableService = tableService;
    }

    @Override
    public Map<String, Object> renderTableList(List<ApiTplExcelData> apiTplExcelDataList) {
        List<Table> tableReusltList = new ArrayList<>();
        //根据swagger的接口url进行分组
        Map<String, List<ApiTplExcelData>> swaggerUrlListMap = apiTplExcelDataList.stream().parallel().collect(Collectors.groupingBy(ApiTplExcelData::getSwaggerUrl));
        Iterator<Map.Entry<String, List<ApiTplExcelData>>> swaggerUrlIt = swaggerUrlListMap.entrySet().iterator();
        while (swaggerUrlIt.hasNext()) {
            Map.Entry<String, List<ApiTplExcelData>> entry = swaggerUrlIt.next();
            String swaggerUrl = entry.getKey();
            List<ApiTplExcelData> apiDataList = entry.getValue();
            //获取出解析后的tableList
            List<Table> tableList = tableService.getTableList(swaggerUrl);
            //将list转为map,提高查询时间复杂度
            Map<String, List<Table>> urlTableListMap = tableList.stream().parallel().collect(Collectors.groupingBy(Table::getUrl));

            apiDataList.stream().parallel().forEach(apiDataItem -> {
                List<Table> subTables = urlTableListMap.get(apiDataItem.getApiPathUrl());
                if (CollectionUtils.isNotEmpty(subTables)) {
                    subTables.forEach(subTableItem -> {
                        if (StringUtils.isNotBlank(apiDataItem.getTag())) {
                            subTableItem.setTag(apiDataItem.getTag());
                        }
                        if (StringUtils.isNotBlank(apiDataItem.getTitle())) {
                            subTableItem.setTitle(apiDataItem.getTitle());
                        }
                        if (StringUtils.isNotBlank(apiDataItem.getApiMethod())) {
                            //excel中传了请求method时，判断请求类型是否匹配
                            if (apiDataItem.getApiMethod().equalsIgnoreCase(subTableItem.getRequestType())) {
                                //请求类型和url都匹配
                                tableReusltList.add(subTableItem);
                            } else {
                                log.info("[renderTableList] method不匹配.url:{} method1:{} method2:{}", apiDataItem.getApiPathUrl(), apiDataItem.getApiMethod(), subTableItem.getRequestType());
                            }
                        } else {
                            tableReusltList.add(subTableItem);
                        }
                    });
                }
            });
        }
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, List<Table>> tableMap = tableReusltList.stream().parallel().collect(Collectors.groupingBy(Table::getTitle));
        resultMap.put("tableMap", new TreeMap<>(tableMap));
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTitle("api接口文档");
        LocalDate now = LocalDate.now();
        tableInfo.setVersion(now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth());
        resultMap.put("info", tableInfo);
        return resultMap;
    }
}
