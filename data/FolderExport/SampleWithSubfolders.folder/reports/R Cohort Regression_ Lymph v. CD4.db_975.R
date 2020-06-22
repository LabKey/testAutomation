library(Cairo);
Cairo(file="${imgout:labkeyl_cairo.png}", type="png");
options(echo=TRUE);
lct <- Sys.getlocale("LC_TIME"); Sys.setlocale("LC_TIME", "C")
labkey.data$date<-as.POSIXct(strptime(labkey.data$date, "%Y-%m-%d"))
#participantvisit_physical_exam_apxwtkg

cohort1<-subset(labkey.data, participantvisit_status_assessment_cohort =='1')
cohort2<-subset(labkey.data, participantvisit_status_assessment_cohort =='2')

#labkey.data
x1=cohort1$cd4
y1=cohort1$lymphocytes
plot(x1, y1, ylab="Lymphocytes (cells/mm3)", xlab="CD4+ (cells/mm3)", main="Lymphocytes vs CD4 By Cohort", ylim=range(labkey.data$lymphocytes), xlim=range(labkey.data$cd4), pch=15, col="red") 
# fit a line to the points
fit1 <- lm(y1 ~ x1) 
summary(fit1) 
abline(fit1, col="red")

x2=cohort2$cd4
y2=cohort2$lymphocytes
points(x2, y2, pch=15, col="blue") 
# fit a line to the points
fit2 <- lm(y2 ~ x2) 
summary(fit2) 
abline(fit2, col="blue")

legend("topleft", c("HIV Negative Cohort", "HIV Acute Cohort"), col=c("blue","red"), pch=c(15, 15));