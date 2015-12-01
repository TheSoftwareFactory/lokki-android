/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.avatar;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import cc.softwarefactory.lokki.android.models.Person;

import java.lang.ref.WeakReference;

public class AvatarLoader {

    private static final String TAG = "AvatarLoader";

    public void load(Person person, ImageView imageView) {
        if (person == null || person.getEmail() == null) return;
        Log.d(TAG, "1) load: " + person.getEmail());
        if (!cancelPotentialWork(person, imageView)) {
            return;
        }

        Log.d(TAG, "load: Creating new task");
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        final WeakReference<BitmapWorkerTask> taskReference = new WeakReference<>(task);
        imageView.setTag(taskReference);
        task.execute(person);
    }

    class BitmapWorkerTask extends AsyncTask<Person, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        private Person data;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Person... params) {

            Log.d(TAG, "BitmapWorkerTask: doInBackground: " + params[0]);
            data = params[0];
            return processData(data);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            Log.d(TAG, "BitmapWorkerTask: onPostExecute");
            if (isCancelled()) {
                return;
            }

            final ImageView imageView = imageViewReference.get();
            if (imageView == null) {
                return;
            }

            BitmapWorkerTask task = getTaskFromView(imageView);

            if (this == task) {
                imageView.setImageBitmap(bitmap);
                imageView.setTag(data);
            }
        }
    }

    private Bitmap processData(Person person) {
        Log.d(TAG, "processData");
        return person.getPhoto();
    }

    private static boolean cancelPotentialWork(Person person, ImageView imageView) {

        BitmapWorkerTask task = getTaskFromView(imageView);

        if (task == null) {
            Log.e(TAG, "cancelPotentialWork: No task associated with the ImageView, or an existing task was cancelled"); // No task associated with the ImageView, or an existing task was cancelled
            return true;
        }

        if (!task.data.getEmail().equals(person.getEmail())) {
            Log.e(TAG, "cancelPotentialWork: Cancel previous task"); // Cancel previous task
            task.cancel(true);
            return true;
        }
        Log.e(TAG, "cancelPotentialWork: The same work is already in progress"); // The same work is already in progress
        return false;
    }

    private static BitmapWorkerTask getTaskFromView(ImageView imageView) {


        if (imageView == null || !(imageView.getTag() instanceof WeakReference)) {
            return null;
        }


        return ((WeakReference<BitmapWorkerTask>)(imageView.getTag())).get();
    }

}
