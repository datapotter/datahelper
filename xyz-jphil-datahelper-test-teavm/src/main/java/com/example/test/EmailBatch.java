package com.example.test;

import lombok.Data;
import xyz.jphil.datahelper.DataHelper;
import java.util.List;

@DataHelper
@Data
public class EmailBatch implements EmailBatch_I<EmailBatch> {
    String batchId;
    String entityName;
    Integer urlCount;
    List<String> trackingIds;
}
