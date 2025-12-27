/*
 * Copyright 2010 Phil Burk, Mobileer Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jsyn.devices;

import com.jsyn.devices.javasound.JavaSoundAudioDevice;

/**
 * Create a device appropriate for the platform.
 * 
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
public class AudioDeviceFactory {
    private static AudioDeviceManager instance;

    /**
     * Use a custom device interface. Overrides the selection of a default device manager.
     * 
     * @param instance
     */
    public static void setInstance(AudioDeviceManager instance) {
        AudioDeviceFactory.instance = instance;
    }

    /**
     * Try to load JPortAudio or JavaSound devices.
     * 
     * @return A device supported on this platform.
     */
    public static AudioDeviceManager createAudioDeviceManager() {
        loadJavaSoundDevice();
        return instance;
    }

    private static void loadJavaSoundDevice() {
        if (instance == null) {
            try {
                instance = new JavaSoundAudioDevice();
            } catch (Throwable e) {
                System.err.println("Could not load JavaSound device. " + e);
            }
        }
    }

}
