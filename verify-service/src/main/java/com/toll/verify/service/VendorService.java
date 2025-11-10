/*
package com.toll.verify.service;


import com.toll.common.model.TagInfo;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VendorService {

    private static final Map<String, TagInfo> MOCK_TAGS = Map.of(
            "T2011", new TagInfo("T2011", "MH12AB3456", "LIGHT", 1200.50, "John Doe", "ACTIVE"),
            "T2012", new TagInfo("T2012", "MH14XY1111", "HEAVY", 450.75, "Ravi Kumar", "ACTIVE"),
            "T2013", new TagInfo("T2013", "MH15ZZ9999", "LIGHT", 0.00, "Priya Singh", "BLOCKED")
    );

    public TagInfo getTagInfo(String tagId) {
        return MOCK_TAGS.getOrDefault(
                tagId,
                new TagInfo(tagId, "UNKNOWN", "UNKNOWN", 0.0, "UNKNOWN", "NOT_FOUND")
        );
    }
}
*/
