setlocal
cd ..\sampledata
rd /q /s study\assaydata
del /q study\plate*.tsv
del /q study\StudyDump.class
del /q study\*.log
del /q study\v068.job.ser
del /q xarfiles/expVerify/*.log
rd /q /s xarfiles\ms2pipe\bov_sample\xars
del /q xarfiles\ms2pipe\bov_sample\xtandem\test1\*.log
rd /q /s xarfiles\ms2pipe\bov_sample\xtandem\test2
del /q xarfiles\ms2pipe\default_input.xml
del /q xarfiles\ms2pipe\protocols\mass_spec\TestMS2Protocol.xml
del /q xarfiles\ms2pipe\protocols\xtandem\test2.xml
