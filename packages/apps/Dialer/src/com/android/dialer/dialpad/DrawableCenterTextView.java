//Add By chaoyongan to compose message ui (QL1700) SW00070146 2014-8-8
package com.android.dialer.dialpad;

import android.widget.TextView;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import android.util.Log;

public class DrawableCenterTextView extends TextView {
	// / Added by guofeiyao
	private float leftOffset;
	// / End

    public DrawableCenterTextView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawableCenterTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawableCenterTextView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable[] drawables = getCompoundDrawables();
        if (drawables != null) {
            Drawable drawableLeft = drawables[0];
            if (drawableLeft != null) {
                float textWidth = getPaint().measureText(getText().toString());
                int drawablePadding = getCompoundDrawablePadding();
                int drawableWidth = 0;
                drawableWidth = drawableLeft.getIntrinsicWidth();
                float bodyWidth = textWidth + drawableWidth + drawablePadding;
				
				// / Modified by guofeiyao,there are so many bugs in the original codes,
				//   how terrible they are! :(
				//   Now,this is a hack for AL812
				//Log.e("DrawableCenterTextView","getWidth():"+getWidth()+" bodyWidth:"+bodyWidth);
				if ( 308 > bodyWidth ) {
				     if ( bodyWidth > getWidth() ) {
    				  	  bodyWidth = getWidth();
				     }
                //canvas.translate((getWidth() - bodyWidth) / 2 + 8, 0);
                leftOffset = (getWidth() - bodyWidth) / 2 - 8;
				canvas.translate(leftOffset , 0);

				}
				// / End
            }
        }
        super.onDraw(canvas);
    }
}
