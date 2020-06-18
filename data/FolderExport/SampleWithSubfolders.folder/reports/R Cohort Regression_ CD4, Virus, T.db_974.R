# Caveats for these plots:
# - Y Axis. The left-most y axis insists on having the largest maximum.
# --- In the first plot, Viral Load has the largest max (and thus is
# --- graphed on the left y axis), but in the second plot CD4 has the 
# --- largest max (and thus is graphed on the left).  Frustratingly, the 
# --- left and right y axis switch places between the top and bottom 
# --- graphs.  Ideally, CD4 would always be on the left.
# - X Axis.  The x axis marks time in months, but ideally it would
# --- show weeks.  R provides functions that allow you to easily 
# --- extract months (but not weeks!) from POSIX time values.

library(Cairo);
Cairo(file="${imgout:cd4viraltime.png}", type="png");
options(echo=TRUE);
par(las=0,xaxs="r",mar=c(6, 6, 6, 6))
lct <- Sys.getlocale("LC_TIME"); Sys.setlocale("LC_TIME", "C")

t <-strptime(labkey.data$date, "%Y-%m-%d")
labkey.data$date<-as.POSIXct(t)
cohort1<-subset(labkey.data, participantvisit_status_assessment_cohort =='1')
cohort2<-subset(labkey.data, participantvisit_status_assessment_cohort =='2')

x1=cohort1$date
y1=cohort1$participantvisit_hiv_test_results_hivloadquant
y12=cohort1$cd4
plot(x1, y1, xaxt="n", ylab="", xlab="Month", 
main="HIV Acute Cohort: Viral Load and CD4 Count vs. Time", 
ylim=c(min(labkey.data$cd4), max(labkey.data$participantvisit_hiv_test_results_hivloadquant)), 
xlim=range(labkey.data$date), pch=15, col="red", las = 1)
 
#fit a line to the points
fit1 <- lm(y1 ~ x1) 
summary(fit1) 
abline(fit1, col="red")
par(new=TRUE)
mtext("HIV Viral Load (copies/ml)", side=2, line = 4.5)

plot(x1, y12, xaxt="n",yaxt="n",ylab="", xlim=range(labkey.data$date), pch=15, col="blue") 
# fit a line to the points
fit12 <- lm(y12 ~ x1) 
summary(fit12) 
abline(fit12, col="blue")
axis(4)
mtext("CD4 (cells/mm3)", side=4, line = 2.7)

minyear<-as.POSIXlt(min(t))$year
minmon<-as.POSIXlt(min(t))$mon
month.number <- function (x) {12*(as.POSIXlt(x)$year-minyear)+as.POSIXlt(x)$mon-minmon} 
r <- as.POSIXct(range(t))
seqr<-seq(r[1], r[2], by="month")
axis.POSIXct(1, at=seqr, format="%b", labels=seq(0, (length(seqr)-1), 1))

legend("topright", 
c("Viral Load", "CD4 Count"), 
lty=c("solid", "solid"), 
col=c("red", "blue"), pch=c(15, 15));
dev.off()

Cairo(file="${imgout:cd4viraltime2.png}", type="png");
par(las=0,xaxs="r",mar=c(6, 6, 6, 6))

x2=cohort2$date
y2=cohort2$participantvisit_hiv_test_results_hivloa
y22=cohort2$cd4

plot(x2, y22, xaxt="n", ylab="", xlab="Month", main="HIV Negative Cohort: Viral Load and CD4 Count vs. Time", 
ylim=c(min(cohort2$participantvisit_hiv_test_results_hivloadquant), max(cohort2$cd4)), xlim=range(cohort2$date), pch=15, col="blue", las = 1)
# fit a line to the points
fit22 <- lm(y22 ~ x2) 
summary(fit22) 
abline(fit22, col="blue")
par(new=TRUE)
mtext("CD4 (cells/mm3)", side=2, line = 4.5)
par(new=TRUE)

# The plot() method below produces a y2-specific range on the right y axis, but
# the new range makes the graph very hard to read. Thus I've stuck with points().
#plot(x2, y2, xaxt="n",yaxt="n",ylab="", xlim=range(cohort2$date), pch=15, col="red") 
points(x2, y2, pch=15, col="red") 
# fit a line to the points
fit2 <- lm(y2 ~ x2) 
summary(fit2) 
abline(fit2, col="red")
axis(4)
mtext("HIV Viral Load (copies/ml)", side=4, line = 4)

t <-strptime(cohort2$date, "%Y-%m-%d")
minyear<-as.POSIXlt(min(t))$year
minmon<-as.POSIXlt(min(t))$mon
month.number <- function (x) {12*(as.POSIXlt(x)$year-minyear)+as.POSIXlt(x)$mon-minmon} 
r <- as.POSIXct(range(t))
seqr<-seq(r[1], r[2], by="month")
seq(r[1], r[2], by="month")
seq(0, (length(seqr)-2))
axis.POSIXct(1, at=seq(r[1], r[2], by="month"), format="%b", labels=seq(0, (length(seqr)-2), 1))

legend("topleft", 
c("Viral Load", "CD4 Count"), 
lty=c("solid", "solid"), 
col=c("red", "blue"), pch=c(15, 15));
dev.off()