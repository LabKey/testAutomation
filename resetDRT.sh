#!/bin/sh
cd ../../sampledata
rm -rf study/assaydata
rm -rf study/plate*.tsv
rm -rf study/StudyDump.class
rm -rf study/*.log
rm -rf study/v068.job.ser
rm -rf xarfiles/expVerify/*.log
rm -rf xarfiles/ms2pipe/bov_sample/xars
rm -rf xarfiles/ms2pipe/bov_sample/xtandem/test1/*.log
rm -f xarfiles/ms2pipe/bov_sample/xtandem/test2
rm -rf xarfiles/ms2pipe/default_input.xml
rm -rf xarfiles/ms2pipe/protocols/mass_spec/TestMS2Protocol.xml
rm -rf xarfiles/ms2pipe/protocols/xtandem/test2.xml
