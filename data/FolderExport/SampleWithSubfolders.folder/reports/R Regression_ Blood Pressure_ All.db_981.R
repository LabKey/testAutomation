library(Cairo);
Cairo(file="${imgout:diastol_v_systol_figure.png}", type="png");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
 main="Diastolic vs. Systolic Pressures: All Visits", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();

png(filename="${imgout:diastol_v_systol_figure2.png}");
plot(labkey.data$diastolicbloodpressure, labkey.data$apxbpsys, 
 main="Diastolic vs. Systolic Pressures: All Visits", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();