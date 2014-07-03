/*
 * Created on Thursday, April 28 2011 22:10
 */
package com.jogamp.opencl.util;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLResource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.*;
import static com.jogamp.opencl.CLDevice.Type.*;

/**
 * Utility for organizing multiple {@link CLContext}s.
 *
 * @author Michael Bien
 */
public class CLMultiContext implements CLResource {

    private final List<CLContext> contexts;
    private boolean released;

    private CLMultiContext() {
        contexts = new ArrayList<CLContext>();
    }

    /**
     * Creates a multi context with all devices of the specified platforms.
     */
    public static CLMultiContext create(final CLPlatform... platforms) {
        return create(platforms, ALL);
    }

    /**
     * Creates a multi context with all devices of the specified platforms and types.
     */
    @SuppressWarnings("unchecked")
    public static CLMultiContext create(final CLPlatform[] platforms, final CLDevice.Type... types) {
        return create(platforms, CLDeviceFilters.type(types));
    }

    /**
     * Creates a multi context with all matching devices of the specified platforms.
     */
    @SuppressWarnings("unchecked")
    public static CLMultiContext create(final CLPlatform[] platforms, final Filter<CLDevice>... filters) {

        if(platforms == null) {
            throw new NullPointerException("platform list was null");
        }else if(platforms.length == 0) {
            throw new IllegalArgumentException("platform list was empty");
        }

        final List<CLDevice> devices = new ArrayList<CLDevice>();
        for (final CLPlatform platform : platforms) {
            devices.addAll(asList(platform.listCLDevices(filters)));
        }
        return create(devices);
    }

    /**
     * Creates a multi context with the specified devices.
     * The devices don't have to be from the same platform.
     */
    public static CLMultiContext create(final Collection<CLDevice> devices) {

        if(devices.isEmpty()) {
            throw new IllegalArgumentException("device list was empty");
        }

        final Map<CLPlatform, List<CLDevice>> platformDevicesMap = filterPlatformConflicts(devices);

        // create contexts
        final CLMultiContext mc = new CLMultiContext();
        for (final Map.Entry<CLPlatform, List<CLDevice>> entry : platformDevicesMap.entrySet()) {
            final List<CLDevice> list = entry.getValue();
            // one context per device to workaround driver bugs
            for (final CLDevice device : list) {
                final CLContext context = CLContext.create(device);
                mc.contexts.add(context);
            }
        }

        return mc;
    }

    /**
     * Creates a multi context with specified contexts.
     */
    public static CLMultiContext wrap(final CLContext... contexts) {
        final CLMultiContext mc = new CLMultiContext();
        mc.contexts.addAll(asList(contexts));
        return mc;
    }

    /**
     * filter devices; don't allow the same device to be used in more than one platform.
     * example: a CPU available via the AMD and Intel SDKs shouldn't end up in two contexts
     */
    private static Map<CLPlatform, List<CLDevice>> filterPlatformConflicts(final Collection<CLDevice> devices) {

        // FIXME: devicename-platform is used as unique device identifier - replace if we have something better

        final Map<CLPlatform, List<CLDevice>> filtered = new HashMap<CLPlatform, List<CLDevice>>();
        final Map<String, CLPlatform> used = new HashMap<String, CLPlatform>();

        for (final CLDevice device : devices) {

            final String name = device.getName();

            final CLPlatform platform = device.getPlatform();
            final CLPlatform usedPlatform = used.get(name);

            if(usedPlatform == null || platform.equals(usedPlatform)) {
                if(!filtered.containsKey(platform)) {
                    filtered.put(platform, new ArrayList<CLDevice>());
                }
                filtered.get(platform).add(device);
                used.put(name, platform);
            }

        }
        return filtered;
    }


    /**
     * Releases all contexts.
     * @see CLContext#release()
     */
    @Override
    public void release() {
        if(released) {
            throw new RuntimeException(getClass().getSimpleName()+" already released");
        }
        released = true;
        for (final CLContext context : contexts) {
            context.release();
        }
        contexts.clear();
    }

    public List<CLContext> getContexts() {
        return Collections.unmodifiableList(contexts);
    }

    /**
     * Returns a list containing all devices used in this multi context.
     */
    public List<CLDevice> getDevices() {
        final List<CLDevice> devices = new ArrayList<CLDevice>();
        for (final CLContext context : contexts) {
            devices.addAll(asList(context.getDevices()));
        }
        return devices;
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [" + contexts.size()+" contexts, "
                                               + getDevices().size()+ " devices]";
    }



}
