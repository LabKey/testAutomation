library(Cairo);
Cairo(file="${imgout:labkeyl_cairo.png}", type="png");
options(echo=TRUE);
lct <- Sys.getlocale("LC_TIME"); Sys.setlocale("LC_TIME", "C")
t <-strptime(labkey.data$date, "%Y-%m-%d")
labkey.data$date<-as.POSIXct(t)

cohort1<-subset(labkey.data, participantvisit_status_assessment_cohort =='1')
cohort2<-subset(labkey.data, participantvisit_status_assessment_cohort =='2')

x1=cohort1$date
y1=cohort1$participantvisit_hiv_test_results_hivloadquant

plot(x1, y1, xlab="Months", xaxt="n", ylab="Viral Load (cells/mm3)", main="Viral Load By Cohort Over Time", xlim=range(labkey.data$date), ylim=range(labkey.data$participantvisit_hiv_test_results_hivloadquant), pch=15, col="red") 

# For an x axis measured in years, use the following call to plot instead of the one above, plus cut the later call to axis(). 
# plot(x1, y1, xlab="Time", ylab="Viral Load (cells/mm3)", main="Viral Load By Cohort Over Time", xlim=range(labkey.data$date), ylim=range(labkey.data$participantvisit_hiv_test_results_hivloadquant), pch=15, col="red") 

# fit a line to the points
fit1 <- lm(y1 ~ x1) 
summary(fit1) 
abline(fit1, col="red")

x2=cohort2$date
y2=cohort2$participantvisit_hiv_test_results_hivloadquant
points(x2, y2, pch=15, col="blue") 
# fit a line to the points
fit2 <- lm(y2 ~ x2) 
summary(fit2) 
abline(fit2, col="blue")
 
minyear<-as.POSIXlt(min(t))$year
minmon<-as.POSIXlt(min(t))$mon
month.number <- function (x) {12*(as.POSIXlt(x)$year-minyear)+as.POSIXlt(x)$mon-minmon} 
r <- as.POSIXct(range(t))
seqr<-seq(r[1], r[2], by="month")
axis.POSIXct(1, at=seqr, format="%b", labels=seq(0, (length(seqr)-1), 1))

legend("topleft", c("HIV Negative Cohort", "HIV Acute Cohort"), col=c("blue","red"), pch=c(15, 15));