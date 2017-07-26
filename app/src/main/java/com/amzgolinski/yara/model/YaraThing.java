package com.amzgolinski.yara.model;

import com.fasterxml.jackson.databind.node.NullNode;

import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;


public class YaraThing extends Thing implements Votable {

  private String mId;
  private YaraThing.Type mType;
  private static final String SUBREDDIT_PREFIX = "t5_";
  private static final String SUBMISSION_PREFIX = "t3_";

  public enum Type {
    SUBMISSION,
    SUBREDDIT
  }

  public YaraThing(String id, YaraThing.Type type) {
    super(NullNode.getInstance());
    mId = id;
    mType = type;
  }

  @Override
  public Integer getScore() {
    return null;
  }

  @Override
  public VoteDirection getVote() {
    return null;
  }

  @Override
  public String getId() {
    return mId;
  }

  @Override
  public String getFullName() {
    String name = mId;
    if (mType.equals(Type.SUBMISSION)) {
      name = SUBMISSION_PREFIX + mId;
    } else {
      name = SUBREDDIT_PREFIX + mId;
    }
    return name;
  }

  public YaraThing.Type getType() {
    return mType;
  }

}
