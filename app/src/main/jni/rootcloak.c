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

// open
#include <fcntl.h>



#define DEBUG_LOGS 0 // 1 to enable logs


FILE *fopen(const char *path, const char *mode) {
    if (DEBUG_LOGS) {
        printf("In our own fopen, opening %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "fopen(): path %s, mode %s", path, mode);
    }

    char *fname = basename(path);

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
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

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "open(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }

    static int *(*original_open)(const char *path, int oflag, ... ) = NULL;
    if (!original_open) {
        original_open = dlsym(RTLD_NEXT, "open");
    }
    return (int) original_open(path, oflag);
}



int stat(const char *path, struct stat *buf) {
    if (DEBUG_LOGS) {
        printf("In our own stat, stat()'ing %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "stat(): path %s", path);
    }

    char *fname = basename(path);

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
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
    return (int) original_stat(path, buf);
}

int lstat(const char *path, struct stat *buf) {
    if (DEBUG_LOGS) {
        printf("In our own lstat, lstat()'ing %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "stat(): path %s", path);
    }

    char *fname = basename(path);

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
        if (DEBUG_LOGS) {
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "stat(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_lstat)(const char*, struct stat*) = NULL;
    if (!original_lstat) {
        original_lstat = dlsym(RTLD_NEXT, "lstat");
    }
    return (int) original_lstat(path, buf);
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

    unsigned int found = 0;
    do {
        if (strcasecmp("su", ret->d_name) == 0 || strcasecmp("daemonsu", ret->d_name) == 0 || strcasecmp("superuser.apk", ret->d_name) == 0) {
            if (DEBUG_LOGS) {
                printf("Found su file, reading next...");
            }
            ret = original_readdir(dirp);
            if (DEBUG_LOGS) {
                printf(" done!\n");
            }
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

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "execve(): Hiding su file %s", filename);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_execve)(const char *filename, char *const argv[], char *const envp[]) = NULL;
    if (!original_execve) {
        original_execve = dlsym(RTLD_NEXT, "execve");
    }
    return (int) original_execve(filename, argv, envp);
}


char *strstr(const char *haystack, const char *needle) {
    if (DEBUG_LOGS) {
        printf("In our own strstr, opening haystack %s, needle %s\n", haystack, needle);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "strstr(): haystack %s, needle %s", haystack, needle);
    }

    if (strcasecmp("su", needle) == 0 || strcasestr(needle, "supersu") != NULL ||
        strcasestr(needle, "rootkeeper") != NULL || strcasestr(needle, "hidemyroot") != NULL) {
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

int access(const char *pathname, int mode) {
    if (DEBUG_LOGS) {
        printf("In our own access, access()'ing %s\n", pathname);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "access(): path %s", pathname);
    }

    char *fname = basename(pathname);

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
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
    return (int) original_access(pathname, mode);
}
