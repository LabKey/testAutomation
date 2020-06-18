library(lattice);
png(filename="${imgout:a}", width=900);
plot.new();
xyplot(systolicbloodpressure~ date| participantid, data=labkey.data,
 type="a", scales=list(draw=FALSE));
update(trellis.last.object(),
 strip = strip.custom(strip.names = FALSE, strip.levels = TRUE),
 main = "Systolic Pressure vs. Time, By Participant", 
 ylab="Systolic Pressure", xlab="");
dev.off();

png(filename="${imgout:b}", width=900);
plot.new();
xyplot(weight_kg ~ date| participantid, data=labkey.data,
 type="a", scales=list(draw=FALSE));
update(trellis.last.object(),
 strip = strip.custom(strip.names = FALSE, strip.levels = TRUE),
 main = "Weight vs. Time, By Participant", 
 ylab="Weight (kg)", xlab="");
dev.off();