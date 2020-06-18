library(Cairo);
data_means <- aggregate(labkey.data, list(ParticipantID = 
 labkey.data$participantid), mean, na.rm = TRUE);
Cairo(file="${imgout:multiplot.png}", type="png")
op <- par(mfcol = c(2, 2)) # 2 x 2 pictures on one plot
c11 <- plot(data_means$diastolicbloodpressure, data_means$weight_kg, , 
 xlab="Diastolic Blood Pressure (mm Hg)", ylab="Weight (kg)",
 mfg=c(1, 1))
abline(lsfit(data_means$diastolicbloodpressure, data_means$weight_kg))
c21 <- plot(data_means$diastolicbloodpressure, data_means$systolicbloodpressure, , 
 xlab="Diastolic Blood Pressure (mm Hg)", 
 ylab="Systolic Blood Pressure (mm Hg)", mfg= c(2, 1))
abline(lsfit(data_means$diastolicbloodpressure, data_means$systolicbloodpressure))
c21 <- plot(data_means$diastolicbloodpressure, data_means$pulse, , 
 xlab="Diastolic Blood Pressure (mm Hg)", 
 ylab="Pulse Rate (Beats/Minute)", mfg= c(1, 2))
abline(lsfit(data_means$diastolicbloodpressure, data_means$pulse))
c21 <- plot(data_means$diastolicbloodpressure, data_means$temp_c, , 
 xlab="Diastolic Blood Pressure (mm Hg)", 
 ylab="Temperature (Degrees C)", mfg= c(2, 2))
abline(lsfit(data_means$diastolicbloodpressure, data_means$temp_c))
par(op); #Restore graphics parameters
dev.off();