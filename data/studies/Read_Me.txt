3/31/21
Below is a list of the study archive files (zip files) in this folder and the tests that may (note the word may) use them.
For example AltIdStudy is imported in StudyBaseTest, however that does not mean that all the test classes the derive from StudyBaseTest use that imported study. Most do not, but this does give you a starting point of where to look if you change a study archive.
Also there are no guarantees on the accuracy of the list of test classes, new classes may have been added after this was written.

I suspect this will go stale at some point and will need to be updated or replaced with something better.


testAutomation/data/studies/

AltIdStudy.folder.zip
	StudyBaseTest.java
		AncillaryStudyFromSpecimenRequestTest.java (DailyC)
		AncillaryStudyTest.java (DailyC)
		DataViewsPermissionsTest.java (DailyC)
		ExtraKeyStudyTest.java (DailyC)
		ParticipantListTest.java (DailyB)
		    DataViewsTest.java (DailyC)
		QuerySnapshotTest.java (BVT)
		ReportAndDatasetNotificationTest.java (DailyC)
		ReportTest.java
			DataReportsTest.java (DailyB)
			GenericChartsTest.java
				BarPlotTest.java (DailyC)
				BoxPlotTest.java (DailyC)
				LinePlotTest.java (DailyC)
				PieChartTest.java (DailyC)
				ScatterPlotTest.java (DailyC)
			NonStudyReportsTest.java (DailyC)
			ParticipantReportTest.java (DailyC)
			PivotQueryTest.java (DailyB)
			PlatformDeveloperAndTrustedAnalystTest.java (Git)
			ProgressReportTest.java (DailyC)
			ReportSecurityTest.java (DailyC)
			TimeChartTest.java
				TimeChartAPITest.java (DailyC)
				TimeChartDateBasedTest.java (DailyC)
				TimeChartVisitBasedTest.java (DailyC)
		SearchTest.java
			SearchTestDefault.java (DailyC)
			SearchTestMMap.java (Search/Weekly)
			SearchTestNIOFS.java (Search/Weekly)
			SearchTestSimpleFS.java (Search/Weekly)
		SpecimenBaseTest.java
			AliquotTest.java (DailyC)
			CustomizeEmailTemplateTest.java (DailyC)
			SpecimenCustomizeTest.java (DailyC)
			SpecimenExportTest.java (DailyC)
			SpecimenImportTest.java (DailyC)
			SpecimenTest.java (DailyC)
		SpecimenMultipleImportTest.java (DailyC)
		StudyCheckForReloadTest.java (?)
		StudyDatasetIndexTest.java (DailyC)
		StudyDatasetReloadTest.java (DailyC)
		StudyDataspaceTest.java (DailyC)
		StudyDemoModeTest.java (DailyC)
		StudyMergeParticipantsTest.java (DailyC)
		StudyReloadColumnInferenceTest.java (DailyC)
		StudyReloadTest.java (DailyC)
		StudyScheduleTest.java (DailyC)
		StudySimpleExportTest.java (DailyC)
		StudyTest.java (DailyC)
		StudyVisitTagTest.java (DailyC)
		TimeChartImportTest.java (DailyC)
	StudyDatasetsTest.java (DailyA)
	TruncationTest.java (DailyA)

CAVDTestStudy.folder.zip
	CAVDStudyTest in customModules

CohortStudy.zip
	CohortTest.java (DailyB)
	StudyProtocolDesignerTest.java (DailyB)

LabkeyDemoStudy.zip
	ColumnChartTest.java (DailyB)
	DatasetPublishTest.java (DailyA)
	DataViewsReportOrderingTest.java (DailyA)
	FieldEditorRowSelectionActionTest.java (DailyA)
	GenericMeasurePickerTest.java (DailyA)
	LinkedSchemaTest.java (DailyA)
	MenuBarTest.java (DailyA)
	PipelineCancelTest.java (DailyB)
	PivotQueryTest.java (DailyB)
	PremiumBlueGreenStudyTest.java (BlueGreen)
	ProgressReportTest.java (DailyC)
	StudyDatasetDefaultViewTest.java (DailyA)
	StudyImportPerfTest.java (Perf)
	StudyMergeParticipantsTest.java (DailyC)
	TimeChartTest.java
		TimeChartAPITest.java (DailyC)
		TimeChartDateBasedTest.java (DailyC)
		TimeChartVisitBasedTest.java (DailyC)
	TriggerScriptTest.java (DailyC)

LabkeyDemoStudyWith100Tables.zip
	Not used?

LabkeyDemoStudyWith200Tables.zip
	SchemaBrowserPerfTest.java (Perf)

LabkeyDemoStudyWith300Tables.zip
	Not used?

LabkeyDemoStudyWithCharts.folder.zip
	ReportThumbnailTest.java (DailyB)
	StudySimpleExportTest.java (DailyC)

ReportDatasetNotifyTest.folder.zip
	ReportAndDatasetNotificationTest.java (DailyC)

SpecimenCustomizeStudy.folder.zip
	SpecimenCustomizeTest.java (DailyC)

StudySecurityProject.folder.zip
	StudySecurityTest.java (DailyC)

StudyWithDatasetIndex.folder.zip
	StudyDatasetIndexTest.java (DailyC)

StudyWithDatasetSharedIndex.folder.zip
	StudyDatasetIndexTest.java (DailyC)

StudyWithDemoBit.folder.zip
	S3ImportTest.java (S3)
	StudyDatasetReloadTest.java (DailyC)

StudyWithoutDemoBit.folder.zip
	StudyDatasetReloadTest.java (DailyC)

TimeChartTesting.folder.zip
	TimeChartImportTest.java (DailyC)

studyshell.zip
	SCHARPStudyTest.java (DailyA)


testAutomation/data/studies/Dataspace

DataspaceStudyTest-Study1B.zip
	StudyDataspaceTest.java (DailyC)

DataspaceStudyTest-Study2B.zip
	StudyDataspaceTest.java (DailyC)

DataspaceStudyTest-Study5.zip
	StudyDataspaceTest.java (DailyC)


testAutomation/data/studies/ExtraKeyStudy
    FilesQueryTest.java (DailyC)