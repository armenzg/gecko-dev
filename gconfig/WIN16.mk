#
# The contents of this file are subject to the Netscape Public License
# Version 1.0 (the "NPL"); you may not use this file except in
# compliance with the NPL.  You may obtain a copy of the NPL at
# http://www.mozilla.org/NPL/
#
# Software distributed under the NPL is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the NPL
# for the specific language governing rights and limitations under the
# NPL.
#
# The Initial Developer of this code under the NPL is Netscape
# Communications Corporation.  Portions created by Netscape are
# Copyright (C) 1998 Netscape Communications Corporation.  All Rights
# Reserved.
#

#
# win16_3.11.mk -- Make configuration for Win16
#
# This file configures gmake to build the Win16 variant of
# NSPR 2.0. This file has the function of two files commonly
# used on other platforms, for example: winnt.mk and
# winnt4.0.mk. ... The packaging is easier and there is only
# one variant of the Win16 target.
# 
# Win16 is built using the Watcom C/C++ version 11.0
# compiler. You gotta set up the compiler first. Follow the
# directions in the manual (Ha! ... really, its not a
# problem). The Watcom compiler depends on a few environment
# variables; these environment variables define where the
# compiler components are installed; they must be set before
# running the make.
# 
# Notes:
# OS_CFLAGS is the command line options for the compiler when
#   building the .DLL object files.
# OS_EXE_CFLAGS is the command line options for the compiler
#   when building the .EXE object files; this is for the test
#   programs.
# the macro OS_CFLAGS is set to OS_EXE_CFLAGS inside of the
#   makefile for the pr/tests directory. ... Hack.
# 
# 
# 
# 

# -- configuration -----------------------------------------

DEFAULT_COMPILER = wcc

CC           = wcc
CCC          = wcl
LINK         = wlink
AR           = wlib
AR          += -q $@
RC           = wrc.exe
RC          += /r /dWIN16=1 /bt=windows
RANLIB       = echo
BSDECHO      = echo
NSINSTALL_DIR  = $(GDEPTH)/gconfig/nsinstall
NSINSTALL      = nsinstall
INSTALL	     = $(NSINSTALL)
MAKE_OBJDIR  = mkdir
MAKE_OBJDIR += $(OBJDIR)
XP_DEFINE   += -DXP_PC
LIB_SUFFIX   = lib
DLL_SUFFIX   = dll

ifdef BUILD_OPT
	OPTIMIZER   = -oneatx -oh -oi -ei -3 -fpi87 -fp3
else
	OPTIMIZER  += -d2 -hc -DDEBUG
#	OPTIMIZER  += -d2 -hw -DDEBUG
#	LDFLAGS  += -DEBUG -DEBUGTYPE:CV
endif

# XXX FIXME: I doubt we use this.  It is redundant with
# SHARED_LIBRARY.
ifdef DLL
	DLL := $(addprefix $(OBJDIR)/, $(DLL))
endif


#
# $(CPU_ARCH) has been commented out so that its contents
# are not added to the WIN16_?.OBJ names thus expanding
# them beyond the 8.3 character limit for this platform.
#
#CPU_ARCH       = x386
#
# added "-s" to avoid dependency on watcom's libs (e.g. on _STK)
# added "-zt3" for compatibility with MSVC's "/Gt3" option
#
OS_CFLAGS     += -ml -3 -bd -zc -zu -bt=windows -s -zt3 -d_X86_ -dWIN16 -d_WINDLL 
#OS_EXE_CFLAGS += -ml -3 -bt=windows -d_X86_ -dWIN16
OS_LIB_FLAGS   = -c -iro

# Name of the binary code directories
OS_DLL_OPTION = CASEEXACT
OS_DLLFLAGS  =
OS_LIBS      =
W16_EXPORTS  = #

#
#  The following is NOT needed for the NSPR 2.0 library.
#

OS_CFLAGS += -d_WINDOWS -d_MSC_VER=700
