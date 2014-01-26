
    static final DynamicLibraryBundle dynamicLookupHelper;
    protected static final CLProcAddressTable addressTable;

    static {
      addressTable = new CLProcAddressTable();
      if(null==addressTable) {
        throw new RuntimeException("Couldn't instantiate ALProcAddressTable");
      }
  
      dynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                                  public DynamicLibraryBundle run() {
                                      final DynamicLibraryBundle bundle =  new DynamicLibraryBundle(new CLDynamicLibraryBundleInfo());
                                      if(null==bundle) {
                                        throw new RuntimeException("Null CLDynamicLookupHelper");
                                      }
                                      if(!bundle.isToolLibLoaded()) {
                                        throw new RuntimeException("Couln't load native CL library");
                                      }
                                      if(!bundle.isLibComplete()) {
                                        throw new RuntimeException("Couln't load native CL/JNI glue library");
                                      }
                                      addressTable.reset(bundle);
                                      return bundle;
                                  } } );
    }

    public static CLProcAddressTable getCLProcAddressTable() { return addressTable; }

    static long clGetExtensionFunctionAddress(long clGetExtensionFunctionAddressHandle, java.lang.String procname)
    {
        if (clGetExtensionFunctionAddressHandle == 0) {
            throw new RuntimeException("Passed null pointer for method \"clGetExtensionFunctionAddress\"");
        }
        return dispatch_clGetExtensionFunctionAddressStatic(procname, clGetExtensionFunctionAddressHandle);
    }

    public CLAbstractImpl() {
    }

    /** Entry point (through function pointer) to C language function: <br> <code> void* clGetExtensionFunctionAddress(const char *  fname); </code>    */
    long clGetExtensionFunctionAddress(String fname)  {

        final long __addr_ = addressTable._addressof_clGetExtensionFunctionAddress;
        if (__addr_ == 0) {
            throw new UnsupportedOperationException("Method \"clGetExtensionFunctionAddress\" not available");
        }
        return dispatch_clGetExtensionFunctionAddressStatic(fname, __addr_);
    }

    /** Entry point (through function pointer) to C language function: <br> <code> void* clGetExtensionFunctionAddress(const char *  fname); </code>    */
    private static native long dispatch_clGetExtensionFunctionAddressStatic(String fname, long procAddress);

