/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package name.wonjoon.sohn.pedometer;


/**
 * Calculates and displays the distance walked.  
 * @author Levente Bagi
 */
public class ThresholdNotifier implements StepListener, SpeakingTimer.Listener {

    public interface Listener {
        public void valueChanged(float value);
        public void passValue();
    }
    private Listener mListener;

    private double mThreshold = 0;

    PedometerSettings mSettings;
    Utils mUtils;

    boolean mIsMetric;
    float mStepLength;

    public ThresholdNotifier(Listener listener, PedometerSettings settings, Utils utils) {
        mListener = listener;
        mUtils = utils;
        mSettings = settings;
        reloadSettings();
    }
    public void setThreshold(float threshold) {
        mThreshold = threshold;
        notifyListener();
    }

    public void resetValues() {
        mThreshold = 0;
    }

    public void reloadSettings() {
        mIsMetric = mSettings.isMetric();
        mStepLength = mSettings.getStepLength();
        notifyListener();
    }

    public void onStep() {
        
        notifyListener();
    }
    private void notifyListener() {
        mListener.valueChanged((float)mThreshold);
    }


    public void passValue() {
        // Callback of StepListener - Not implemented
    }

    public void speak() {
        if (mSettings.shouldTellDistance()) {
            if (mThreshold >= .001f) {
                mUtils.say(("" + (mThreshold + 0.000001f)).substring(0, 4) + (mIsMetric ? " kilometers" : " miles"));
                // TODO: format numbers (no "." at the end)
            }
        }
    }


}

