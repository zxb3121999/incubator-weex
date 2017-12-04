/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.weex.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;

import com.taobao.weex.ui.component.WXText;
import com.taobao.weex.ui.view.gesture.WXGesture;
import com.taobao.weex.ui.view.gesture.WXGestureObservable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * TextView wrapper
 */
public class WXTextView extends View implements WXGestureObservable, IWXTextView,
                                                IRenderStatus<WXText>, IRenderResult<WXText> {

  private WeakReference<WXText> mWeakReference;
  private WXGesture wxGesture;
  private Layout textLayout;
  private boolean mIsLinkClick = false;
  private boolean mIsLabelSet = false;
  private CharSequence mString;
  public WXTextView(Context context) {
    super(context);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.save();
    Layout layout= getTextLayout();
    if(layout!=null){
      canvas.translate(getPaddingLeft(),getPaddingTop());
      layout.draw(canvas);
    }
    canvas.restore();
  }
  public void IsLinkClick(boolean mIsLinkClick) {
    this.mIsLinkClick = mIsLinkClick;
  }

  private void findClickAble() {
    mString = getText();
    if (TextUtils.isEmpty(mString)) return;
    ClickableSpan[] links = ((Spannable) mString).getSpans(0, getText().length(), ClickableSpan.class);
    if (links.length > 0) {
      spanList.clear();
      for (ClickableSpan span : links) {
        SpanBean bean = new SpanBean();
        bean.span = span;
        int start = ((Spannable) getText()).getSpanStart(span);
        int line = textLayout.getLineForOffset(start);
        int end = ((Spannable) getText()).getSpanEnd(span);
        int endLine = textLayout.getLineForOffset(end);
        float left = textLayout.getPrimaryHorizontal(start);
        float right = textLayout.getSecondaryHorizontal(end);
        if (line == endLine) {
          Rect bounds = new Rect();
          textLayout.getLineBounds(line, bounds);
          bounds.left = (int) left;
          bounds.right = (int) right;
          bean.start = bounds;
        } else {
          Rect startBounds = new Rect();
          textLayout.getLineBounds(line, startBounds);
          startBounds.left = (int) left;
          Rect endBounds = new Rect();
          textLayout.getLineBounds(line, endBounds);
          endBounds.right = (int) right;
          bean.start = startBounds;
          bean.end = endBounds;
        }
        spanList.add(bean);
      }
    }
  }

  private List<SpanBean> spanList = new ArrayList<>();
  private SpanBean clickBean;
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = false;
    if (mIsLinkClick) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        clickBean = null;
        if (textLayout != null && !TextUtils.isEmpty(getText()) && !TextUtils.equals(getText(), mString)) {
          findClickAble();
        }
        for (SpanBean span : spanList) {
          float x = event.getX();
          float y = event.getY();
          boolean click;
          if (span.end == null) {
            click = x >= span.start.left && x <= span.start.right && y >= span.start.top && y <= span.start.bottom;
          } else {
            click = (x >= span.start.left && y >= span.start.top && y <= span.start.bottom) || (x <= span.end.right && y >= span.end.top && y <= span.end.bottom);
          }
          if (click) {
            clickBean = span;
            result = true;
            break;
          }
        }
      } else if (event.getAction() == MotionEvent.ACTION_UP && clickBean != null) {
        clickBean.span.onClick(this);
      }
    }
    result = super.onTouchEvent(event);
    if (wxGesture != null) {
      result |= wxGesture.onTouch(this, event);
    }
    return result;
  }

  @Override
  public void registerGestureListener(WXGesture wxGesture) {
    this.wxGesture = wxGesture;
  }

  @Override
  public CharSequence getText() {
    return textLayout.getText();
  }

  public Layout getTextLayout() {
    return textLayout;
  }

  public void setTextLayout(Layout layout) {
    this.textLayout = layout;
    if(layout!=null && !mIsLabelSet){
      setContentDescription(layout.getText());
    }
    if (mWeakReference != null) {
      WXText wxText = mWeakReference.get();
      if (wxText != null) {
        wxText.readyToRender();
      }
    }
  }

  public void setAriaLabel(String label){
    if(!TextUtils.isEmpty(label)){
      mIsLabelSet = true;
      setContentDescription(label);
    }else{
      mIsLabelSet = false;
      if(textLayout != null){
        setContentDescription(textLayout.getText());
      }
    }

  }

  @Override
  public void holdComponent(WXText component) {
    mWeakReference = new WeakReference<>(component);
  }

  @Nullable
  @Override
  public WXText getComponent() {
     return null != mWeakReference ? mWeakReference.get() : null;
  }
  class SpanBean {
    ClickableSpan span;
    Rect start;
    Rect end;
  }
}
