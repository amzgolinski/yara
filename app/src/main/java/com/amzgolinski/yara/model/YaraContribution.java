package com.amzgolinski.yara.model;


import com.fasterxml.jackson.databind.node.NullNode;

public class YaraContribution extends net.dean.jraw.models.Contribution {

  private String mId;
  private static final String SUBMISSION_PREFIX = "t3_";

  public YaraContribution(String id) {
    super(NullNode.getInstance());
    mId = SUBMISSION_PREFIX + id;
  }

  @Override
  public String getFullName() {
    return mId;
  }

}
