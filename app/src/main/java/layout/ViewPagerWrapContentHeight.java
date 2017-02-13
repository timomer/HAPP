package layout;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * Special thanks to Daniel López Lacalle for his response
 * (http://stackoverflow.com/questions/8394681/android-i-am-unable-to-have-viewpager-wrap-content/20784791#20784791)
 * Android ViewPager does the respect WrapContent as its fragments have not loaded, this c
 * */
public class ViewPagerWrapContentHeight extends ViewPager {

    public ViewPagerWrapContentHeight(Context context) {
        super(context);
    }

    public ViewPagerWrapContentHeight(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int height = 200;
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int h = child.getMeasuredHeight();
            if(h > height) height = h;
        }

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
