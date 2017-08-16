package com.amzgolinski.yara.util;


public class StringUtils {

  public static final String EMPTY_STRING = "";

  public static String removeHtmlSpacing(String html) {
    html = html.replace("<div class=\"md\">", "");
    html = html.replace("</div>", "");
    html = html.replace("<p>", "");
    html = html.replace("</p>", "");
    return html;
  }

  public static boolean isStringEmpty(String toTest) {
    return (toTest == null || toTest.equals(EMPTY_STRING));
  }

}
