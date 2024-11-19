package org.bytedeco.zixi.presets;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;
import org.bytedeco.javacpp.tools.*;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.NoException;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.presets.javacpp;
import org.bytedeco.javacpp.tools.BuildEnabled;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;
import org.bytedeco.javacpp.tools.Logger;


@Properties(inherit = javacpp.class,
    value = { 
        @Platform( value="macosx-x86_64",
        includepath = {"/usr/include/", "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include/",
                   "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/"},
        include = {
                "sys/_types/_iovec_t.h",
                "sys/socket.h",
                "netinet/in.h",
                "zixi_definitions.h",
        		"zixi_feeder_interface.h"
        		}
          ,link = "zixiFeeder"
      
    ),
    @Platform(value = "linux-x86_64", 
              includepath = {"/usr/include", "/usr/include/x86_64-linux-gnu"},
              exclude={"bits/socket.h", "linux/uio.h"},
              include={ 
                        "linux/uio.h",
                        "sys/uio.h",
                        "bits/sockaddr.h",
                        "bits/socket.h", 
                        "sys/socket.h",
                        "netinet/in.h",
                        "zixi_definitions.h", 
                        "zixi_feeder_interface.h"
                },
              link = "zixiFeeder"
    ),
    
    
    @Platform(value = "linux-arm64", 
              includepath = {"/usr/include", "/usr/include/aarch64-linux-gnu"},
              exclude={"bits/socket.h", "linux/uio.h"},
              include={ 
                        "linux/uio.h",
                        "sys/uio.h",
                        "bits/sockaddr.h",
                        "bits/socket.h", 
                        "sys/socket.h",
                        "sys/syslog.h",
                        "srt/logging_api.h", 
                        "srt/srt.h"},
              link = "srt@.1.4.4"
    )
    },
    target = "org.bytedeco.zixi.feeder",
    global = "org.bytedeco.zixi.global.feeder"
)
public class zixiFeeder implements InfoMapper 
{
    
    public void map(InfoMap infoMap) 
    {
        
        
       infoMap
        .put(new Info("__BEGIN_DECLS").cppText("#define __BEGIN_DECLS"))
        .put(new Info("_WIN32").define(false))
        .put(new Info("__END_DECLS").cppText("#define __END_DECLS"))
        .put(new Info("ZIXI_FILE_HANDLE").cppText("#define ZIXI_FILE_HANDLE FILE*"))  //.cppTypes().annotations()) // .cppTypes("FILE*")) 
        .put(new Info("INVALID_FILE_HANDLE").cppTypes().annotations())
        .put(new Info("ZIXI_FEEDER_SUPPORT_RTMP").cppText("true"))
        .put(new Info("__darwin_va_list").cppTypes("char*"))
        .put(new Info("__gnuc_va_list").cppTypes("char*"))
        .put(new Info("sa_family_t", "__uint8_t")
                .cast().valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))
        .put(new Info("!defined(_POSIX_C_SOURCE) || defined(_DARWIN_C_SOURCE)",
                  "__APPLE__", "__DARWIN_UNIX03").define(true))
        .put(new Info("WIN32","defined (ANDROID)").define(false))
        .put(new Info(
        		//"nl_item", 
        		"socklen_t"
        		//, "useconds_t", "errno_t", "blksize_t",
               // "dev_t", "id_t"
        		, "gid_t", "uid_t"
        		, "pid_t"
        		//, "cpu_type_t", "cpu_subtype_t", "sigset_t",
               // "__darwin_suseconds_t", "__darwin_sigset_t", "exception_behavior_t", "exception_mask_t",
               // "mach_port_t", "thread_state_flavor_t", "integer_t", "__int32_t", "__uint32_t"
        		, "u_int32_t"
        		, "sae_associd_t"
        		, "sae_connid_t"
                ,"in_addr_t"
                )
          .cast().valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))
          .put(new Info(
        		  //"greg_t", "blkcnt_t", "blkcnt64_t",
                "__off_t", 
                "__off64_t", 
                "__kernel_size_t",
                //"rlim64_t",
                //"__darwin_ino64_t", 
                "__int64_t"
        		  //, "__uint64_t", "u_int64_t"
        		  )
          .cast().valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))
   
        .put(new Info(
                        //"nlink_t", "mode_t", "__int16_t", 
                        "__uint16_t"
                        ,"in_port_t"
                        //, "u_int16_t"
                        )
                        .cast().valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))
        .put(new Info(
                            //"__DARWIN_ALIAS", "__DARWIN_STRUCT_STAT64_TIMES", "__DARWIN_STRUCT_STAT64", "_NLS_PRIVATE",
                            //"_STRUCT_TIMESPEC", "_STRUCT_TIMEVAL", "_STRUCT_SIGALTSTACK", "_STRUCT_UCONTEXT",
                            "__extension__","__extern_inline" , "__ss_aligntype",
                            //"__header_always_inline", "__inline", "__mode__",
                            //"__nonnull", "_Nullable", 
                            "__restrict"
                            ,
                            "SRT_API"
                            ,"__REDIRECT"
                           // ,
                           // "__darwin_va_list"
                            //, "__CLOCK_AVAILABILITY", "__OS_AVAILABILITY_MSG",
                            //"__DYLDDL_DRIVERKIT_UNAVAILABLE", "__IOS_PROHIBITED", "__TVOS_PROHIBITED", "__WATCHOS_PROHIBITED",
                            //"ru_first", "ru_last", "sv_onstack"
                            ).annotations().cppTypes()) 
        .put(new Info("!defined(_POSIX_C_SOURCE) || defined(_DARWIN_C_SOURCE)",
                            "__APPLE__", "__DARWIN_UNIX03").define(true))
        .put(new Info("SYSLOG_NAMES", " __DARWIN_C_LEVEL >= __DARWIN_C_FULL", "__USE_FILE_OFFSET64", "__USE_EXTERN_INLINES", "__USE_MISC").define(false))
                              
        .put(new Info("_SS_PAD2SIZE","_SS_PADSIZE"
        		  //, "WCOREFLAG"
        		  ).cppTypes("int").translate(false))
        .put(new Info("SAE_ASSOCID_ALL").cppText("#define SAE_ASSOCID_ALL ((int)(-1ULL))"))
        .put(new Info("SAE_CONNID_ALL").cppText("#define SAE_CONNID_ALL ((int)(-1ULL))"))
        .put(new Info("DLL_EXPORT").cppTypes().annotations())
        .put(new Info("socket.h").linePatterns("__BEGIN_DECLS", "__END_DECLS").skip())
        .put(new Info("cmsghdr::__flexarr"//, "getwd", "getpw", "lchmod", "mktemp", "revoke", "setlogin",
                           //  "sigblock", "siggetmask", "sigsetmask", "sigreturn", "sigstack(sigstack*, sigstack*)"
                           ).skip())
        .put(new Info("bool").define().cast().valueTypes("boolean").pointerTypes("BoolPointer", "boolean..."))
        //.put(new Info("!__USE_KERNEL_IPV6_DEFS").define(false))
        .put(new Info("s6_addr", "IN6ADDR_ANY_INIT","IN6ADDR_LOOPBACK_INIT").cppTypes().annotations())

       ;
    }
}
