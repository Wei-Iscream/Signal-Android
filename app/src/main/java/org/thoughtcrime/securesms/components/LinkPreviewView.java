package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.linkpreview.LinkPreview;
import org.thoughtcrime.securesms.linkpreview.LinkPreviewRepository;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.ImageSlide;
import org.thoughtcrime.securesms.mms.SlidesClickedListener;
import org.thoughtcrime.securesms.util.ThemeUtil;

import okhttp3.HttpUrl;

/**
 * The view shown in the compose box that represents the state of the link preview.
 */
public class LinkPreviewView extends FrameLayout {

  private static final int TYPE_CONVERSATION = 0;
  private static final int TYPE_COMPOSE      = 1;

  private ViewGroup             container;
  private OutlinedThumbnailView thumbnail;
  private TextView              title;
  private TextView              site;
  private View                  divider;
  private View                  closeButton;
  private View                  spinner;
  private TextView              noPreview;

  private int                  type;
  private int                  defaultRadius;
  private CornerMask           cornerMask;
  private Outliner             outliner;
  private CloseClickedListener closeClickedListener;

  public LinkPreviewView(Context context) {
    super(context);
    init(null);
  }

  public LinkPreviewView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.link_preview, this);

    container     = findViewById(R.id.linkpreview_container);
    thumbnail     = findViewById(R.id.linkpreview_thumbnail);
    title         = findViewById(R.id.linkpreview_title);
    site          = findViewById(R.id.linkpreview_site);
    divider       = findViewById(R.id.linkpreview_divider);
    spinner       = findViewById(R.id.linkpreview_progress_wheel);
    closeButton   = findViewById(R.id.linkpreview_close);
    noPreview     = findViewById(R.id.linkpreview_no_preview);
    defaultRadius = getResources().getDimensionPixelSize(R.dimen.thumbnail_default_radius);
    cornerMask    = new CornerMask(this);
    outliner      = new Outliner();

    outliner.setColor(ThemeUtil.getThemedColor(getContext(), R.attr.conversation_item_image_outline_color));

    if (attrs != null) {
      TypedArray typedArray   = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.LinkPreviewView, 0, 0);
      type = typedArray.getInt(R.styleable.LinkPreviewView_linkpreview_type, 0);
      typedArray.recycle();
    }

    if (type == TYPE_COMPOSE) {
      container.setBackgroundColor(Color.TRANSPARENT);
      container.setPadding(0, 0, 0, 0);
      divider.setVisibility(VISIBLE);
      closeButton.setVisibility(VISIBLE);

      closeButton.setOnClickListener(v -> {
        if (closeClickedListener != null) {
          closeClickedListener.onCloseClicked();
        }
      });
    }

    setWillNotDraw(false);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (type == TYPE_COMPOSE) return;

    cornerMask.mask(canvas);
    outliner.draw(canvas);
  }

  public void setLoading() {
    title.setVisibility(GONE);
    site.setVisibility(GONE);
    thumbnail.setVisibility(GONE);
    spinner.setVisibility(VISIBLE);
    noPreview.setVisibility(INVISIBLE);
  }

  public void setNoPreview(@Nullable LinkPreviewRepository.Error customError) {
    title.setVisibility(GONE);
    site.setVisibility(GONE);
    thumbnail.setVisibility(GONE);
    spinner.setVisibility(GONE);
    noPreview.setVisibility(VISIBLE);
    noPreview.setText(getLinkPreviewErrorString(customError));
  }

  public void setLinkPreview(@NonNull GlideRequests glideRequests, @NonNull LinkPreview linkPreview, boolean showThumbnail) {
    title.setVisibility(VISIBLE);
    site.setVisibility(VISIBLE);
    thumbnail.setVisibility(VISIBLE);
    spinner.setVisibility(GONE);
    noPreview.setVisibility(GONE);

    title.setText(linkPreview.getTitle());

    HttpUrl url = HttpUrl.parse(linkPreview.getUrl());
    if (url != null) {
      site.setText(url.topPrivateDomain());
    }

    if (showThumbnail && linkPreview.getThumbnail().isPresent()) {
      thumbnail.setVisibility(VISIBLE);
      thumbnail.setImageResource(glideRequests, new ImageSlide(getContext(), linkPreview.getThumbnail().get()), type == TYPE_CONVERSATION, false);
      thumbnail.showDownloadText(false);
    } else {
      thumbnail.setVisibility(GONE);
    }
  }

  public void setCorners(int topLeft, int topRight) {
    cornerMask.setRadii(topLeft, topRight, 0, 0);
    outliner.setRadii(topLeft, topRight, 0, 0);
    thumbnail.setCorners(topLeft, defaultRadius, defaultRadius, defaultRadius);
    postInvalidate();
  }

  public void setCloseClickedListener(@Nullable CloseClickedListener closeClickedListener) {
    this.closeClickedListener = closeClickedListener;
  }

  public void setDownloadClickedListener(SlidesClickedListener listener) {
    thumbnail.setDownloadClickListener(listener);
  }

  private  @StringRes static int getLinkPreviewErrorString(@Nullable LinkPreviewRepository.Error customError) {
    return customError == LinkPreviewRepository.Error.GROUP_LINK_INACTIVE ? R.string.LinkPreviewView_this_group_link_is_not_active
                                                                          : R.string.LinkPreviewView_no_link_preview_available;
  }

  public interface CloseClickedListener {
    void onCloseClicked();
  }
}
