package org.lineageos.dspvolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class VolumeListenerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }

        if (intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, 0)
                == AudioManager.STREAM_MUSIC) {
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int current = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
            audioManager.setParameters("volume_change=" + current + ";flags=8");
        }
    }
}
