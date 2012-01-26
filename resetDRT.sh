#!/bin/sh

#
# Copyright (c) 2008-2012 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
