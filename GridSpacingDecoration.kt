import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.jetbrains.annotations.NotNull;

/**
 * RecyclerView的item间隔实现，支持item与item、item与RecyclerView之间的间距实现
 * <p>1.不支持自定义RecyclerView.LayoutManager</p>
 * <p>2.间隔只能是透明色</p>
 * <p>3.默认会将RecyclerView的clipToPadding属性设置为false</p>
 *
 * eg:
 * GridSpacingDecoration.Builder(this)
 *                 .setOrientation(RecyclerView.VERTICAL)
 *                 .setItemHorizontalSpacing(TypedValue.COMPLEX_UNIT_DIP, dp5)
 *                 .setItemVerticalSpacing(dp15)
 *                 .setRcvPadding(dp15)
 *                 .setRcvPaddingVertical(dp0)
 *                 .setRcvPaddingHorizontal(dp0)
 *                 .setRcvPaddingBottom(dp0)
 *                 .setNoEdgeType(MyRecyclerAdapter.TYPE_BIG)
 *                 .attach(recyclerView)
 */
@SuppressWarnings({"unused"})
public class GridSpacingDecoration extends RecyclerView.ItemDecoration {

    private static final String TAG = "GridSpacingDecoration";

    //<editor-fold desc="属性">
    /**
     * RecyclerView布局方向
     */
    private int mOrientation = RecyclerView.VERTICAL;
    /**
     * RecyclerView中item的垂直间距
     */
    private int mItemVerticalSpacing;
    /**
     * RecyclerView中item的水平间距
     */
    private int mItemHorizontalSpacing;
    /**
     * RecyclerView与item的左侧水平间距
     */
    private int mRcvPaddingStart;
    /**
     * RecyclerView与item的顶部垂直间距
     */
    private int mRcvPaddingTop;
    /**
     * RecyclerView的PaddingEnd
     */
    private int mRcvPaddingEnd;
    /**
     * RecyclerView与item的底部垂直间距
     */
    private int mRcvPaddingBottom;
    /**
     * 与RecyclerView无间距的item类型列表
     */
    private int[] mNoEdgeTypes;
    //</editor-fold>

    //<editor-fold desc="构造函数">
    private GridSpacingDecoration() {

    }
    //</editor-fold>

    //<editor-fold desc="内部处理逻辑">
    @Override
    public void getItemOffsets(@NotNull Rect outRect, @NotNull View view, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null) {
            return;
        }
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        try {
            int verticalSpacing = mItemVerticalSpacing, horizontalSpacing = mItemHorizontalSpacing;
            boolean isHorizontal = mOrientation == RecyclerView.HORIZONTAL;

            boolean isRTL = isRTL(parent);
            int itemCount = adapter.getItemCount();
            int position = parent.getChildAdapterPosition(view);
            int spanCount = getSpanCount(layoutManager); //总列数
            int spanSize; //此Item占用的列数
            int columnIndex; //此Item所在列索引

            if (RecyclerView.NO_POSITION == position) return;
            if (position > itemCount - 1) return;

            if (view.getLayoutParams() instanceof GridLayoutManager.LayoutParams) {
                spanSize = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanSize();
                columnIndex = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
            } else if (view.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                spanSize = 1;
                columnIndex = ((StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
            } else {
                spanSize = 1;
                columnIndex = 0;
            }
            int columnCount = getColumnCount(layoutManager, position, itemCount, spanCount, columnIndex, spanSize);

            boolean inLastGroup = inLastGroup(layoutManager, position, itemCount, spanCount, spanSize);
            boolean inBottomLine = inBottomLine(layoutManager, isHorizontal, position, itemCount, spanCount, spanSize, inLastGroup);
            boolean isNoEdge = isNoEdge(parent, position);

            int left = 0, right = 0, top = 0, bottom = 0;

            // 横向间隔通过设置左右位移实现
            if (isHorizontal) {
                // 水平布局下，直接设置右侧位移为间隔大小，只要不是最后一组数据，都要有
                if (!inLastGroup) {
                    right = horizontalSpacing;
                }
            } else {
                // 垂直布局下，根据在该行的index计算左右偏移
                if (columnCount != 1) {
                    left = columnIndex * horizontalSpacing / columnCount;
                    right = horizontalSpacing - (columnIndex + 1) * horizontalSpacing / columnCount;
                }
            }
            if (isRTL) { //rtl布局下，左右调换
                right = right ^ left;
                left = right ^ left;
                right = right ^ left;
            }

            // 纵向间隔通过设置上下位移实现
            if (isHorizontal) { //水平布局下，底部偏移需要根据该item在列中的index计算
                top = columnIndex * verticalSpacing / columnCount;
                bottom = verticalSpacing - (columnIndex + 1) * verticalSpacing / columnCount;
            } else if (!inBottomLine) { //垂直布局下，只要不在最后一行，直接设置底部偏移为垂直间隔
                bottom = verticalSpacing;
            }

            if (isNoEdge) { //与RecyclerView无间隔的特殊item，需要设置与RecyclerView相邻的一侧位移为RecyclerView在该侧padding的负数
                if (isHorizontal) {
                    outRect.set(left, -mRcvPaddingTop, right, -mRcvPaddingBottom);
                } else {
                    outRect.set(-mRcvPaddingStart, top, -mRcvPaddingEnd, bottom);
                }
            } else {
                outRect.set(left, top, right, bottom);
            }
            Log.d(TAG, "position:" + position + ", isHorizontal:" + isHorizontal + ", isRTL:" + isRTL + ", isNoEdge:" + isNoEdge + ", isNoEdge:" + spanSize + ", spanSize:" + columnCount + ", columnCount:" + columnCount + ", columnIndex:" + columnIndex + ", inBottomLine:" + inBottomLine +  ", inLastGroup:" + inLastGroup + ", outRect:" + outRect);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 检查该是否没有与RecyclerView之间的间隔
     *
     * @param view     RecyclerView
     * @param position position
     */
    private boolean isNoEdge(@NotNull RecyclerView view, int position) {
        RecyclerView.Adapter adapter = view.getAdapter();
        if (adapter == null) {
            return false;
        }
        int type = adapter.getItemViewType(position);
        if (this.mNoEdgeTypes != null) {
            for (int defaultType : this.mNoEdgeTypes) {
                if (defaultType == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断某个item是否在最底部的一行
     *
     * @param layoutManager RecyclerView.LayoutManager
     * @param position      item.position
     * @param itemCount     item总个数
     * @param spanCount     每行(垂直布局)/每列(水平布局)个数
     */
    private boolean inBottomLine(@NotNull RecyclerView.LayoutManager layoutManager, boolean isHorizontal, int position, int itemCount, int spanCount, int spanSize, boolean inLastGroup) {
        if (isHorizontal) {
            if (spanCount == 1) {
                return true;
            }
            if (position == itemCount - 1 && spanCount == spanSize) { //最后一个，如果该item的spanSize=spanCount(独占一列)，则是在最后一行
                return true;
            }
            if (layoutManager instanceof GridLayoutManager) { //非最后一个，则如果下一个item的columnIndex=0，则是在最后一行
                GridLayoutManager manager = (GridLayoutManager) layoutManager;
                return manager.getSpanSizeLookup().getSpanIndex(position + 1, spanCount) == 0;
            }
            return false;
        } else {
            return inLastGroup;
        }
    }

    /**
     * 判断某个item是否在水平布局的最后一列或者垂直布局的最后一行
     *
     * @param layoutManager RecyclerView.LayoutManager
     * @param position      item.position
     * @param itemCount     item总个数
     * @param spanCount     每行(垂直布局)/每列(水平布局)个数
     */
    private boolean inLastGroup(@NonNull RecyclerView.LayoutManager layoutManager, int position, int itemCount, int spanCount, int spanSize) {
        int count = itemCount - 1 - position;
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager manager = (GridLayoutManager) layoutManager;
            for (int i = 1; i <= count; i++) {
                if (manager.getSpanSizeLookup().getSpanIndex(position + i, spanCount) == 0) {
                    return false;
                }
            }
            return true;
        }
        return count < spanCount;
    }


    /**
     * 计算当前position所在行有几列数据
     *
     * @param layoutManager RecyclerView.LayoutManager
     * @param position      item.position
     * @param spanCount     总列数
     * @param columnIndex   item.columnIndex
     * @param spanSize      item.spanSize
     */
    private int getColumnCount(@NotNull RecyclerView.LayoutManager layoutManager, int position, int itemCount, int spanCount, int columnIndex, int spanSize) {
        int maxRemainColumn = spanCount - (columnIndex + 1) - (spanSize - 1); //最大剩余列数
        if (layoutManager instanceof GridLayoutManager) { //只有GridLayoutManager需要计算
            int occupySpan = columnIndex + spanSize; //已占用列数
            if (maxRemainColumn > 0 && position < itemCount - 1) {
                int count = itemCount - 1 - position; //剩余item个数
                for (int i = 1; i < count; i++) {
                    if (occupySpan >= spanCount) {
                        break;
                    }
                    int newPosition = position + i;
                    int newSpanSize = ((GridLayoutManager) layoutManager).getSpanSizeLookup().getSpanSize(newPosition);
                    occupySpan += newSpanSize; //更新占用列数
                    if (occupySpan <= spanCount) {
                        maxRemainColumn -= (newSpanSize - 1); //更新最大剩余列数
                    }
                }
            }
        }
        return maxRemainColumn + (columnIndex + 1);
    }

    /**
     * 根据layoutManager获取列数
     *
     * @param layoutManager RecyclerView.LayoutManager
     */
    private int getSpanCount(@NotNull RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            return ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
        }
        return 1;
    }

    @SuppressLint("ObsoleteSdkInt")
    private boolean isRTL(@NotNull RecyclerView view) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
    //</editor-fold>

    public static class Builder {

        private final Context mContext;
        private final GridSpacingDecoration decoration = new GridSpacingDecoration();

        public Builder(Context context) {
            mContext = context;
        }

        private int applyDimension(int unit, int size) {
            Resources r;

            if (mContext == null) {
                r = Resources.getSystem();
            } else {
                r = mContext.getResources();
            }
            return (int) TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
        }

        /**
         * 设置RecyclerView布局方向
         *
         * @param orientation RecyclerView布局方向
         */
        public Builder setOrientation(@RecyclerView.Orientation int orientation) {
            decoration.mOrientation = orientation;
            return this;
        }

        public Builder setItemSpacing(int dpSize) {
            return setItemSpacing(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        /**
         * 设置RecyclerView中item的间距
         *
         * @param unit 单位 {@link android.util.TypedValue}
         * @param size 间距大小
         */
        public Builder setItemSpacing(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mItemHorizontalSpacing = size;
            decoration.mItemVerticalSpacing = size;
            return this;
        }

        public Builder setItemHorizontalSpacing(int dpSize) {
            return setItemHorizontalSpacing(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        /**
         * 设置RecyclerView中item的水平间距
         *
         * @param unit 单位 {@link android.util.TypedValue}
         * @param size 间距大小
         */
        public Builder setItemHorizontalSpacing(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mItemHorizontalSpacing = size;
            return this;
        }

        public Builder setItemVerticalSpacing(int dpSize) {
            return setItemVerticalSpacing(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        /**
         * 设置RecyclerView中item的垂直间距
         *
         * @param unit 单位 {@link android.util.TypedValue}
         * @param size 间距大小
         */
        public Builder setItemVerticalSpacing(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mItemVerticalSpacing = size;
            return this;
        }

        public Builder setRcvPadding(int dpSize) {
            return setRcvPadding(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        public Builder setRcvPadding(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mRcvPaddingStart = size;
            decoration.mRcvPaddingTop = size;
            decoration.mRcvPaddingEnd = size;
            decoration.mRcvPaddingBottom = size;
            return this;
        }

        public Builder setRcvPaddingHorizontal(int dpSize) {
            return setRcvPaddingStart(dpSize).setRcvPaddingEnd(dpSize);
        }

        public Builder setRcvPaddingHorizontal(int unit, int size) {
            return setRcvPaddingStart(unit, size).setRcvPaddingEnd(unit, size);
        }

        public Builder setRcvPaddingVertical(int dpSize) {
            return setRcvPaddingTop(dpSize).setRcvPaddingBottom(dpSize);
        }

        public Builder setRcvPaddingVertical(int unit, int size) {
            return setRcvPaddingTop(unit, size).setRcvPaddingBottom(unit, size);
        }

        public Builder setRcvPaddingStart(int dpSize) {
            return setRcvPaddingStart(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        public Builder setRcvPaddingStart(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mRcvPaddingStart = size;
            return this;
        }

        public Builder setRcvPaddingTop(int dpSize) {
            return setRcvPaddingTop(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        public Builder setRcvPaddingTop(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mRcvPaddingTop = size;
            return this;
        }

        public Builder setRcvPaddingEnd(int dpSize) {
            return setRcvPaddingEnd(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        public Builder setRcvPaddingEnd(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mRcvPaddingEnd = size;
            return this;
        }

        public Builder setRcvPaddingBottom(int dpSize) {
            return setRcvPaddingBottom(TypedValue.COMPLEX_UNIT_DIP, dpSize);
        }

        public Builder setRcvPaddingBottom(int unit, int size) {
            size = applyDimension(unit, size);
            decoration.mRcvPaddingBottom = size;
            return this;
        }

        /**
         * 设置与RecyclerView没有间隔的item类型
         *
         * @param type item类型，可以是多个
         */
        public Builder setNoEdgeType(int... type) {
            decoration.mNoEdgeTypes = type;
            return this;
        }

        public Builder attach(@NotNull RecyclerView recyclerView) {
            recyclerView.addItemDecoration(decoration);
            recyclerView.setClipToPadding(false);
            int paddingStart = decoration.mRcvPaddingStart;
            if (paddingStart == 0) {
                paddingStart = recyclerView.getPaddingStart();
            }
            int paddingEnd = decoration.mRcvPaddingEnd;
            if (paddingEnd == 0) {
                paddingEnd = recyclerView.getPaddingEnd();
            }
            int paddingTop = decoration.mRcvPaddingTop;
            if (paddingTop == 0) {
                paddingTop = recyclerView.getPaddingTop();
            }
            int paddingBottom = decoration.mRcvPaddingBottom;
            if (paddingBottom == 0) {
                paddingBottom = recyclerView.getPaddingBottom();
            }
            recyclerView.setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom);
            return this;
        }
        
        public void detach(@NotNull RecyclerView recyclerView) {
            recyclerView.removeItemDecoration(decoration);
        }
    }
}
