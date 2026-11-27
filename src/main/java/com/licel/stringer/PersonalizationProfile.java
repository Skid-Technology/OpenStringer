package com.licel.stringer;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.JxltEngine.Expression;
import org.apache.commons.jexl3.MapContext;

public class PersonalizationProfile extends FilteredParams {
    private static final JexlEngine JEXL_ENGINE = new JexlBuilder().silent(true).cache(128).strict(false).create();
    private static final JxltEngine JXLT_ENGINE = JEXL_ENGINE.createJxltEngine();

    private String noticeTemplate = "AV contact email - primary@licelus.com";
    private Expression noticeExpression = JXLT_ENGINE.createExpression(this.noticeTemplate);
    private String protectedByTemplate = "${stringer.version} Stringer (${stringer.buildId})";
    private Expression protectedByExpression = JXLT_ENGINE.createExpression(this.protectedByTemplate);
    private final Map<String, Object> variables = new HashMap<>();

    public PersonalizationProfile() {
        this.variables.put("stringer.version", ProductInfo.getVersion());
        this.variables.put("stringer.buildId", ProductInfo.getBuildId());
    }

    public void put(String key, Object value) {
        this.variables.put(key, value);
    }

    public String evaluateNotice() {
        return String.valueOf(this.noticeExpression.evaluate(new MapContext(this.variables)));
    }

    public void setNoticeTemplate(String template) {
        this.noticeTemplate = template;
        this.noticeExpression = JXLT_ENGINE.createExpression(template);
    }

    public String getNoticeTemplate() {
        return this.noticeTemplate;
    }

    public String evaluateProtectedBy() {
        return String.valueOf(this.protectedByExpression.evaluate(new MapContext(this.variables)));
    }

    public void setProtectedByTemplate(String template) {
        this.protectedByTemplate = template;
        this.protectedByExpression = JXLT_ENGINE.createExpression(template);
    }

    public String getProtectedByTemplate() {
        return this.protectedByTemplate;
    }
}
