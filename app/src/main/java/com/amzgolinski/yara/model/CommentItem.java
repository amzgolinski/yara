package com.amzgolinski.yara.model;

import android.os.Parcel;
import android.os.Parcelable;

import net.dean.jraw.models.CommentNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CommentItem implements Serializable, Parcelable {

  private static final String LOG_TAG = CommentItem.class.getName();

  private String mId;
  private String mSubmissionId;
  private String mAuthor;
  private String mBody;
  private int mScore;
  private int mType;
  private int mMoreCommentsCount;
  private int mDepth;

  public interface CommentType {
    int COMMENT = 1;
    int HAS_MORE_COMMENTS = 2;
    int NO_REPLIES = 3;
  }

  public CommentItem() {

  }

  public CommentItem(String id, String submissionId, String author, String body, int score,
                     int type, int moreCommentsCount, int depth) {
    mId = id;
    mSubmissionId = submissionId;
    mAuthor = author;
    mBody = body;
    mScore = score;
    mType = type;
    mMoreCommentsCount = moreCommentsCount;
    mDepth = depth;
  }

  public CommentItem(Parcel in) {
    super();
    readFromParcel(in);
  }

  public String getId() {
    return mId;
  }

  public int getDepth() {
    return mDepth;
  }

  public int getMoreCommentsCount(){
    return mMoreCommentsCount;
  }

  public String getAuthor() {
    return mAuthor;
  }

  public String getBody() {
    return mBody;
  }

  public int getType() {
    return mType;
  }

  public Integer getScore() {
    return mScore;
  }

  public String getSubmissionId() {
    return mSubmissionId;
  }

  public void setId(String id){
    mId = id;
  }

  public void setDepth(int depth) {
    mDepth = depth;
  }

  public void setAuthor(String author) {
    mAuthor = author;
  }

  public void setBody (String body) {
    mBody = body;
  }

  public void setScore (int score) {
    mScore = score;
  }

  public void setType(int type) {
    mType = type;
  }

  public void setMoreCommentsCount(int commentsCount) {
    mMoreCommentsCount = commentsCount;
  }

  public void setSubmissionId(String submissionId) {
    mSubmissionId = submissionId;
  }

  public static CommentItem fromCommentNode(CommentNode node) {
    // Log.d(LOG_TAG, comment.getBody());
    CommentItem newWrapper = new CommentItem();
    newWrapper.setId(node.getComment().getId());
    newWrapper.setSubmissionId(node.getComment().getSubmissionId());
    newWrapper.setBody(node.getComment().data("body_html"));
    newWrapper.setDepth(node.getDepth());
    newWrapper.setAuthor(node.getComment().getAuthor());
    newWrapper.setScore(node.getComment().getScore());
    newWrapper.setType(CommentType.COMMENT);
    return newWrapper;
  }

  public static CommentItem generateMoreComment(int depth, int count) {
    CommentItem wrapper = new CommentItem();
    wrapper.setDepth(depth);
    wrapper.setType(CommentType.HAS_MORE_COMMENTS);
    wrapper.setMoreCommentsCount(count);
    return wrapper;
  }

  public String toString () {
    return new StringBuilder()
        .append("Id " + getId() + "\n")
        .append("submission id " + getSubmissionId() + "\n")
        .append("Depth: " + Integer.toString(getDepth()) + "\n")
        .append("Author: " + getAuthor() + "\n")
        .append("Body: " + getBody() + "\n")
        .append("Score: " + Integer.toString(getScore()) + "\n")
        .append("Type: " + Integer.toString(getType()) + "\n")
        .append("Count: " + Integer.toString(getMoreCommentsCount()) + "\n").toString();
  }

  public static ArrayList<CommentItem> walkTree(List<CommentNode> commentNodes) {

    ArrayList<CommentItem> toReturn = new ArrayList<>();
    Stack<CommentItem> moreComments = new Stack<>();
    int currentDepth;

    for (CommentNode node : commentNodes) {

      currentDepth = node.getDepth();
      while (!moreComments.empty() && moreComments.peek().getDepth() > currentDepth) {
        //Log.d(LOG_TAG, "Stack Depth: " + moreComments.peek().getDepth() + " current " + currentDepth);
        //Log.d(LOG_TAG, "Popping node with parent: " + moreComments.peek().getId());
        toReturn.add(moreComments.pop());
      }

      // Log.d(LOG_TAG, comment.getBody());
      CommentItem wrapper = CommentItem.fromCommentNode(node);
      toReturn.add(wrapper);

      if (node.hasMoreComments()) {
        int moreCount = node.getMoreChildren().getCount();
        //Log.d(LOG_TAG, "Has " + moreCount + " more comments. ");
        if (moreCount > 0) {
          //Log.d(LOG_TAG, "Adding key: " + node.getComment().getId());
          CommentItem moreComment
              = CommentItem.generateMoreComment(node.getDepth()+1, moreCount);
          moreComment.setId(node.getComment().getFullName());
          moreComment.setSubmissionId(node.getComment().getSubmissionId().substring(3));
          moreComments.add(moreComment);
        }
      }
    }
    return toReturn;
  }

  public static final Parcelable.Creator<CommentItem> CREATOR
      = new Parcelable.Creator<CommentItem>() {
    public CommentItem createFromParcel(Parcel in) {
      return new CommentItem(in);
    }

    public CommentItem[] newArray(int size) {

      return new CommentItem[size];
    }

  };

  public void readFromParcel(Parcel in) {
    mId = in.readString();
    mSubmissionId = in.readString();
    mAuthor = in.readString();
    mBody = in.readString();
    mScore = in.readInt();
    mType = in.readInt();
    mMoreCommentsCount = in.readInt();
    mDepth = in.readInt();

  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeString(mId);
    dest.writeString(mSubmissionId);
    dest.writeString(mAuthor);
    dest.writeString(mBody);
    dest.writeInt(mScore);
    dest.writeInt(mType);
    dest.writeInt(mMoreCommentsCount);
    dest.writeInt(mDepth);
  }

  @Override
  public int describeContents() {
    return 0;
  }

}
