/*
* Initial work and improvements: pyler
* Main code: NHellFire
*/


#define _GNU_SOURCE

// stat
#include <libgen.h>
#include <sys/stat.h>
#include <string.h>

// fopen
#include <stdio.h>

// readdir
#include <dirent.h>

// Required by all
#include <dlfcn.h>
#include <android/log.h>
#include <errno.h>
#include <regex.h>

// open
#include <fcntl.h>

#define DEBUG_LOGS 0 // 1 to enable logs
#define REGEX_FILTER 1 // 0 to disable regex filter

char *rootcloak_strcasestr(const char *haystack, const char *needle) {
    static char *(*original_strcasestr)(const char*, const char*) = NULL;
    if (!original_strcasestr) {
        original_strcasestr = dlsym(RTLD_NEXT, "strcasestr");
    }
    return original_strcasestr(haystack, needle);
}

int rootcloak_strcasecmp(const char *s1, const char *s2) {
    static int (*original_strcasecmp)(const char*, const char*) = NULL;
    if (!original_strcasecmp) {
        original_strcasecmp = dlsym(RTLD_NEXT, "strcasecmp");
    }
    return original_strcasecmp(s1, s2);
}

int fname_is_blacklisted (const char *fname) {
    if (rootcloak_strcasecmp("su", fname) == 0 || rootcloak_strcasecmp("daemonsu", fname) == 0 || rootcloak_strcasecmp("superuser.apk", fname) == 0) {
        return 1;
    }
    return 0;
}

int str_is_blacklisted (const char *needle) {
    if (rootcloak_strcasecmp("su", needle) == 0 || rootcloak_strcasestr(needle, "supersu") != NULL ||
        rootcloak_strcasestr(needle, "rootkeeper") != NULL || rootcloak_strcasestr(needle, "hidemyroot") != NULL ||
        rootcloak_strcasestr(needle, "rootcloak") != NULL || rootcloak_strcasestr(needle, "daemonsu") != NULL ||
        rootcloak_strcasestr(needle, "xposed") != NULL) {
        return 1;
    }
    return 0;
}


FILE *fopen(const char *path, const char *mode) {
    if (DEBUG_LOGS) {
        printf("In our own fopen, opening %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "fopen(): path %s, mode %s", path, mode);
    }

    char *fname = basename(path);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "fopen(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return NULL;
    }

    static FILE *(*original_fopen)(const char*, const char*) = NULL;
    if (!original_fopen) {
        original_fopen = dlsym(RTLD_NEXT, "fopen");
    }
    return original_fopen(path, mode);
}

int open(const char *path, int oflag, ... ) {
    if (DEBUG_LOGS) {
        printf("In our own open, opening %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "open(): path %s", path);
    }

    char *fname = basename(path);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "open(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }

    static int (*original_open)(const char *path, int oflag, ... ) = NULL;
    if (!original_open) {
        original_open = dlsym(RTLD_NEXT, "open");
    }
    int fd = original_open(path, oflag);
    //#if !(defined(__i386__) || defined(__x86_64__))
    if (REGEX_FILTER) {
        if (fd != -1) {
            int status;
            regex_t re;
            
            if (regcomp(&re, "^/proc/[0-9]+/(stat|cmdline)$", REG_EXTENDED | REG_NOSUB) == 0) {
                status = regexec(&re, path, 0, NULL, 0);
                regfree(&re);
                if (status == 0) {
                    if (strcmp(fname, "stat") == 0) {
                        printf("Opening %s\n", path);
                        unsigned char buf[4096];
                        read(fd, buf, sizeof(buf));
                        char *cmd = malloc(4096);
                        sscanf(buf, "%*d (%[^)]) %*[^\n]", cmd);
                        cmd = basename(cmd);
                        if (str_is_blacklisted(cmd)) {
                            if (DEBUG_LOGS) {
                                printf("Found blacklisted process: %s\n", cmd);
                            }
                            close(fd);
                            errno = ENOENT;
                            return -1;
                        }
                        lseek(fd, SEEK_SET, 0);
    
                    } else if (strcmp(fname, "cmdline") == 0) {
                        printf("Opening %s\n", path);
                        unsigned char buf[4096];
                        read(fd, buf, sizeof(buf));
                        char *tmp = basename(buf);
                        if (str_is_blacklisted(tmp)) {
                            if (DEBUG_LOGS) {
                                printf("Found blacklisted process: %s\n", tmp);
                            }
                            close(fd);
                            errno = ENOENT;
                            return -1;
                        }
                        lseek(fd, SEEK_SET, 0);
                    }
                }
            }
        }
    }
    return fd;
}


int stat(const char *path, struct stat *buf) {
    if (DEBUG_LOGS) {
        printf("In our own stat, stat()'ing %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "stat(): path %s", path);
    }

    char *fname = basename(path);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "stat(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_stat)(const char*, struct stat*) = NULL;
    if (!original_stat) {
        original_stat = dlsym(RTLD_NEXT, "stat");
    }
    return original_stat(path, buf);
}

int lstat(const char *path, struct stat *buf) {
    if (DEBUG_LOGS) {
        printf("In our own lstat, lstat()'ing %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "lstat(): path %s", path);
    }

    char *fname = basename(path);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "lstat(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_lstat)(const char*, struct stat*) = NULL;
    if (!original_lstat) {
        original_lstat = dlsym(RTLD_NEXT, "lstat");
    }
    return original_lstat(path, buf);
}

struct dirent *readdir(DIR *dirp) {
    if (DEBUG_LOGS) {
        printf("In our own readdir\n");
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "readdir()");
    }

    static struct dirent *(*original_readdir)(DIR*);
    if (!original_readdir) {
        original_readdir = dlsym(RTLD_NEXT, "readdir");
    }

    struct dirent* ret = original_readdir(dirp);
    if (ret == NULL) {
        return ret;
    }

    if (DEBUG_LOGS) {
        printf("readdir(): d_name = %s\n", ret->d_name);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "readdir(): d_name = %s", ret->d_name);
    }

    int found = 0;
    do {
        if (fname_is_blacklisted(ret->d_name)) {
            if (DEBUG_LOGS) {
                printf("Found su file, reading next...");
            }
            ret = original_readdir(dirp);
            if (DEBUG_LOGS) {
                printf(" done!\n");
            }
            found = 1;
        } else {
            found = 0;
        }
    } while (found == 1 && ret != NULL);


    return ret;
}

int execve(const char *filename, char *const argv[], char *const envp[]) {
    if (DEBUG_LOGS) {
        printf("In our own execve, execve()'ing %s\n", filename);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "execve(): path %s", filename);
    }

    char *fname = basename(filename);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "execl(): Hiding su file %s", filename);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_execve)(const char *filename, char *const argv[], char *const envp[]) = NULL;
    if (!original_execve) {
        original_execve = dlsym(RTLD_NEXT, "execve");
    }
    return original_execve(filename, argv, envp);
}


char *strstr(const char *haystack, const char *needle) {
    if (DEBUG_LOGS) {
        printf("In our own strstr, haystack %s, needle %s\n", haystack, needle);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strstr(): haystack %s, needle %s", haystack, needle);
    }

    if (str_is_blacklisted(needle)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strstr(): Hiding su %s", needle);
        }
        return NULL;
    }

    static char *(*original_strstr)(const char*, const char*) = NULL;
    if (!original_strstr) {
        original_strstr = dlsym(RTLD_NEXT, "strstr");
    }
    return original_strstr(haystack, needle);
}

char *strcasestr(const char *haystack, const char *needle) {
    if (DEBUG_LOGS) {
        printf("In our own strcasestr, haystack %s, needle %s\n", haystack, needle);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strcasestr(): haystack %s, needle %s", haystack, needle);
    }

    if (str_is_blacklisted(needle)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strcasestr(): Hiding su %s", needle);
        }
        return NULL;
    }

    static char *(*original_strcasestr)(const char*, const char*) = NULL;
    if (!original_strcasestr) {
        original_strcasestr = dlsym(RTLD_NEXT, "strcasestr");
    }

    return original_strcasestr(haystack, needle);
}

int access(const char *pathname, int mode) {
    if (DEBUG_LOGS) {
        printf("In our own access, access()'ing %s\n", pathname);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "access(): path %s", pathname);
    }

    char *fname = basename(pathname);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "access(): Hiding su file %s", pathname);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_access)(const char *pathname, int mode) = NULL;
    if (!original_access) {
        original_access = dlsym(RTLD_NEXT, "access");
    }
    return original_access(pathname, mode);
}

int strcasecmp(const char *s1, const char *s2) {
    if (DEBUG_LOGS) {
        printf("In our own strcasecmp, s1 %s, s2 %s\n", s1, s2);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strcasecmp(): s1 %s, s2 %s", s1, s2);
    }

    if (str_is_blacklisted(s2) ) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strcasecmp(): Hiding su %s", s2);
        }
        return -1;
    }

    static int (*original_strcasecmp)(const char*, const char*) = NULL;
    if (!original_strcasecmp) {
        original_strcasecmp = dlsym(RTLD_NEXT, "strcasecmp");
    }
    return original_strcasecmp(s1, s2);
}

FILE *popen(const char *command, const char *type) {
    if (DEBUG_LOGS) {
        printf("In our own popen, popen()'ing %s\n", command);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "popen(): path %s", command);
    }

    char *fname = basename(command);

    if (fname_is_blacklisted(fname)) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "popen(): Hiding su file %s", command);
        }
        errno = ENOENT;
        return NULL;
    }


    static FILE *(*original_popen)(const char*, const char*) = NULL;
    if (!original_popen) {
        original_popen = dlsym(RTLD_NEXT, "popen");
    }
    return original_popen(command, type);
}
