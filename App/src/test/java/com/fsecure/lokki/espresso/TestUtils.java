package com.fsecure.lokki.espresso;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.view.View;

public class TestUtils {

    public void clearAppData(Context targetContext) {
        SharedPreferences.Editor editor = targetContext.getSharedPreferences(targetContext.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }


    public static ViewAction clickScreenPosition(final int x, final int y){
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {

                        final int[] screenPos = new int[2];
                        view.getLocationOnScreen(screenPos);

                        final float screenX = screenPos[0] + x;
                        final float screenY = screenPos[1] + y;
                        float[] coordinates = {screenX, screenY};

                        return coordinates;
                    }
                },
                Press.FINGER);
    }

}
