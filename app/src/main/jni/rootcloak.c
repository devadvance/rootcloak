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

int execl(const char *path, const char *arg, ...) {
    if (DEBUG_LOGS) {
        printf("In our own execl, execl()'ing %s\n", path);
        __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "execl(): path %s", path);
    }

    char *fname = basename(path);

    if (strcasecmp("su", fname) == 0 || strcasecmp("daemonsu", fname) == 0 || strcasecmp("superuser.apk", fname) == 0) {
        if (DEBUG_LOGS) {
            __android_log_print(ANDROID_LOG_INFO, "ROOTCLOAK", "execl(): Hiding su file %s", path);
        }
        errno = ENOENT;
        return -1;
    }


    static int (*original_execl)(const char *path, const char *arg, ...) = NULL;
    if (!original_execl) {
        original_execl = dlsym(RTLD_NEXT, "execl");
    }
    return (int) original_execl(path, arg);
}