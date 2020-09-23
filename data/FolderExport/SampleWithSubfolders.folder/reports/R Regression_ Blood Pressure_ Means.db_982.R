data_means <- aggregate(labkey.data, list(ParticipantID = 
 labkey.data$participantid), mean, na.rm = TRUE);
library(Cairo);
Cairo(file="${imgout:diastol_v_systol_means_figure.png}", type="png");
plot(data_means$diastolicbloodpressure, data_means$systolicbloodpressure, 
 main="Diastolic vs. Systolic Pressures: Mean Value For Each Participant", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(data_means$diastolicbloodpressure, data_means$systolicbloodpressure));
dev.off();