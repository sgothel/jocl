/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.opencl.util.CLBuildConfiguration;
import com.jogamp.opencl.util.CLBuildListener;
import com.jogamp.opencl.util.CLProgramConfiguration;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CLProgramBuilder is a helper for building programs with more complex configurations or
 * building multiple programs with similar configurations.
 * CLProgramBuilder is used to create {@link CLProgramConfiguration}s and {@link CLBuildConfiguration}s.
 * @see CLProgram#prepare()
 * @see #createConfiguration()
 * @see #createConfiguration(com.jogamp.opencl.CLProgram)
 * @see #loadConfiguration(java.io.ObjectInputStream)
 * @see #loadConfiguration(java.io.ObjectInputStream, com.jogamp.opencl.CLContext)
 * @author Michael Bien
 */
public final class CLProgramBuilder implements CLProgramConfiguration, Serializable, Cloneable {

    static final long serialVersionUID = 42;

    private static final byte[] NO_BINARIES = new byte[0];

    private transient CLProgram program;
    private transient Map<CLDevice, byte[]> binariesMap = new LinkedHashMap<CLDevice, byte[]>();

    private String source;

    private final Set<String> optionSet = new LinkedHashSet<String>();
    private final Set<String> defineSet = new LinkedHashSet<String>();


    private CLProgramBuilder() {
        this(null);
    }

    private CLProgramBuilder(final CLProgram program) {
        this(program, null, null);
    }

    private CLProgramBuilder(final CLProgram program, final String source, final Map<CLDevice, byte[]> map) {
        this.program = program;
        this.source = source;
        if(map != null) {
            this.binariesMap.putAll(map);
        }
    }

    /**
     * Creates a new CLBuildConfiguration.
     */
    public static CLBuildConfiguration createConfiguration() {
        return createConfiguration(null);
    }

    /**
     * Creates a new CLProgramConfiguration for this program.
     */
    public static CLProgramConfiguration createConfiguration(final CLProgram program) {
        return new CLProgramBuilder(program);
    }

    /**
     * Loads a CLBuildConfiguration.
     * @param ois The ObjectInputStream for reading the object.
     */
    public static CLBuildConfiguration loadConfiguration(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (CLBuildConfiguration) ois.readObject();
    }

    /**
     * Loads a CLProgramConfiguration containing a CLProgram.
     * The CLProgram is initialized and ready to be build after this method call.
     * This method prefers program initialization from binaries if this fails or if
     * no binaries have been found, it will try to load the program from sources. If
     * this also fails an appropriate exception will be thrown.
     * @param ois The ObjectInputStream for reading the object.
     * @param context The context used for program initialization.
     */
    public static CLProgramConfiguration loadConfiguration(final ObjectInputStream ois, final CLContext context) throws IOException, ClassNotFoundException {
        final CLProgramBuilder config = (CLProgramBuilder) ois.readObject();
        if(allBinariesAvailable(config)) {
            try{
                config.program = context.createProgram(config.binariesMap);
            }catch(final CLException.CLInvalidBinaryException ex) {
                if(config.source != null) {
                    config.program = context.createProgram(config.source);
                }else{
                    throw new IOException("Program configuration contained invalid program binaries and no source.", ex);
                }
            }
        }else if(config.source != null) {
            config.program = context.createProgram(config.source);
        }else{
            throw new IOException("Program configuration did not contain program sources or binaries");
        }
        return config;
    }

    private static boolean allBinariesAvailable(final CLProgramBuilder config) {
        for (final Map.Entry<CLDevice, byte[]> entry : config.binariesMap.entrySet()) {
            if(Arrays.equals(NO_BINARIES, entry.getValue())) {
                return false;
            }
        }
        return config.binariesMap.size() > 0;
    }

    @Override
    public void save(final ObjectOutputStream oos) throws IOException {
        if(program != null) {
            this.source = program.getSource();
            if(program.isExecutable()) {
                binariesMap = program.getBinaries();
            }
        }
        oos.writeObject(this);
    }


    @Override
    public CLProgramBuilder withOption(final String option) {
        optionSet.add(option);
        return this;
    }

    @Override
    public CLProgramBuilder withOptions(final String... options) {
        optionSet.addAll(Arrays.asList(options));
        return this;
    }

    @Override
    public CLProgramBuilder withDefine(final String name) {
        defineSet.add(CLProgram.define(name));
        return this;
    }

    @Override
    public CLProgramBuilder withDefines(final String... names) {
        for (final String name : names) {
            defineSet.add(CLProgram.define(name));
        }
        return this;
    }

    @Override
    public CLProgramBuilder withDefine(final String name, final Object value) {
        defineSet.add(CLProgram.define(name, value.toString()));
        return this;
    }

    @Override
    public CLProgramBuilder withDefines(final Map<String, ? extends Object> defines) {
        for (final Map.Entry<String, ? extends Object> define : defines.entrySet()) {
            final String name = define.getKey();
            final Object value = define.getValue();
            defineSet.add(CLProgram.define(name, value));
        }
        return this;
    }

    @Override
    public CLProgramBuilder forDevice(final CLDevice device) {
        if(!binariesMap.containsKey(device)) {
            binariesMap.put(device, NO_BINARIES);
        }
        return this;
    }

    @Override
    public CLProgramBuilder forDevices(final CLDevice... devices) {
        for (final CLDevice device : devices) {
            forDevice(device);
        }
        return this;
    }

    @Override
    public CLProgram build() {
        return build(program, null);
    }

    @Override
    public CLProgram build(final CLBuildListener listener) {
        return build(program, listener);
    }

    @Override
    public CLProgram build(final CLProgram program) {
        return build(program, null);
    }

    @Override
    public CLProgram build(final CLProgram program, final CLBuildListener listener) {
        if(program == null) {
            throw new NullPointerException("no program has been set");
        }
        final List<String> setup = new ArrayList<String>();
        setup.addAll(optionSet);
        setup.addAll(defineSet);
        final String options = CLProgram.optionsOf(setup.toArray(new String[setup.size()]));
        final CLDevice[] devices = binariesMap.keySet().toArray(new CLDevice[binariesMap.size()]);
        return program.build(listener, options, devices);
    }

    @Override
    public CLProgramBuilder reset() {
        resetOptions();
        resetDefines();
        resetDevices();
        return this;
    }

    @Override
    public CLProgramConfiguration resetDefines() {
        defineSet.clear();
        return this;
    }

    @Override
    public CLProgramConfiguration resetDevices() {
        binariesMap.clear();
        return this;
    }

    @Override
    public CLProgramConfiguration resetOptions() {
        optionSet.clear();
        return this;
    }

    // format: { platform_suffix, num_binaries, (device_name, length, binaries)+ }
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        String suffix = "";

        if(!binariesMap.isEmpty()) {
            final CLDevice device = binariesMap.keySet().iterator().next();
            if(device.isICDAvailable())
                suffix = device.getPlatform().getICDSuffix();
        }

        // empty string if we have no binaries or no devices specified, or if cl_khr_icd isn't supported
        out.writeUTF(suffix);
        out.writeInt(binariesMap.size());   // may be 0

        for (final Map.Entry<CLDevice, byte[]> entry : binariesMap.entrySet()) {
            final CLDevice device = entry.getKey();
            final byte[] binaries = entry.getValue();

            out.writeUTF(device.getName());
            out.writeInt(binaries.length);
            out.write(binaries);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        final String suffix = in.readUTF();  // empty string means no suffix was written; just picks first platform
        CLPlatform platform = null;
        for (final CLPlatform p : CLPlatform.listCLPlatforms()) {
            if(suffix.isEmpty() || p.getICDSuffix().equals(suffix)) {
                platform = p;
                break;
            }
        }

        this.binariesMap = new LinkedHashMap<CLDevice, byte[]>();

        List<CLDevice> devices;
        if(platform != null) {
            devices = new ArrayList<CLDevice>(Arrays.asList(platform.listCLDevices()));
        }else{
            devices = Collections.emptyList();
        }

        final int mapSize = in.readInt();

        for (int i = 0; i < mapSize; i++) {
            final String name = in.readUTF();
            final int length = in.readInt();
            final byte[] binaries = new byte[length];
            in.readFully(binaries);

            for (int d = 0; d < devices.size(); d++) {
                final CLDevice device = devices.get(d);
                if(device.getName().equals(name)) {
                    binariesMap.put(device, binaries);
                    devices.remove(d);
                    break;
                }
            }
        }
    }

    @Override
    public CLProgramBuilder asBuildConfiguration() {
        final CLProgramBuilder builder = new CLProgramBuilder();
        builder.defineSet.addAll(defineSet);
        builder.optionSet.addAll(optionSet);
        return builder;
    }

    @Override
    public CLProgramBuilder clone() {
        final CLProgramBuilder builder = new CLProgramBuilder(program, source, binariesMap);
        builder.defineSet.addAll(defineSet);
        builder.optionSet.addAll(optionSet);
        return builder;
    }

    @Override
    public CLProgram getProgram() {
        return program;
    }

    @Override
    public CLProgramBuilder setProgram(final CLProgram program) {
        this.program = program;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CLProgramBuilder");
        sb.append("{options=").append(optionSet);
        sb.append(", defines=").append(defineSet);
        sb.append(", devices=").append(binariesMap);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CLProgramBuilder that = (CLProgramBuilder) o;

        if (source    != null ? !source.equals(that.source)       : that.source    != null) return false;
        if (defineSet != null ? !defineSet.equals(that.defineSet) : that.defineSet != null) return false;
        if (optionSet != null ? !optionSet.equals(that.optionSet) : that.optionSet != null) return false;

        if(binariesMap != null && that.binariesMap != null) {
            if(binariesMap.size() != that.binariesMap.size()) {
                return false;
            }
            final Iterator<CLDevice> iterator0 = binariesMap.keySet().iterator();
            final Iterator<CLDevice> iterator1 = that.binariesMap.keySet().iterator();
            for (int i = 0; i < binariesMap.size(); i++) {
                final CLDevice device0 = iterator0.next();
                final CLDevice device1 = iterator1.next();
                if(!device0.equals(device1) || !Arrays.equals(binariesMap.get(device0), that.binariesMap.get(device1)))
                    return false;
            }
        }else if(binariesMap != null || that.binariesMap != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = optionSet != null ? optionSet.hashCode() : 0;
        result = 31 * result + (defineSet != null ? defineSet.hashCode() : 0);
        result = 31 * result + (binariesMap != null ? binariesMap.hashCode() : 0);
        return result;
    }

}
