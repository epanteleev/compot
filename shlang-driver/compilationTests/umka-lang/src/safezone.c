#define __USE_MINGW_ANSI_STDIO 1

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "umka_common.h"
#include "umka_runtime.h"


void convToRTLDateTime(RTLDateTime *dest, const struct tm *src)
{
    dest->second    = src->tm_sec;
    dest->minute    = src->tm_min;
    dest->hour      = src->tm_hour;
    dest->day       = src->tm_mday;
    dest->month     = src->tm_mon + 1;
    dest->year      = src->tm_year + 1900;
    dest->dayOfWeek = src->tm_wday + 1;
    dest->dayOfYear = src->tm_yday + 1;
    dest->isDST     = src->tm_isdst != 0;
}

void convFromRTLDateTime(struct tm *dest, const RTLDateTime *src)
{
    dest->tm_sec    = src->second;
    dest->tm_min    = src->minute;
    dest->tm_hour   = src->hour;
    dest->tm_mday   = src->day;
    dest->tm_mon    = src->month - 1;
    dest->tm_year   = src->year - 1900;
    dest->tm_wday   = src->dayOfWeek - 1;
    dest->tm_yday   = src->dayOfYear - 1;
    dest->tm_isdst  = src->isDST;
}

void rtlmemcpy(UmkaStackSlot *params, UmkaStackSlot *result)
{
    void *dest   = umkaGetParam(params, 0)->ptrVal;
    void *src    = umkaGetParam(params, 1)->ptrVal;
    int   count  = umkaGetParam(params, 2)->intVal;

    memcpy(dest, src, count);
}