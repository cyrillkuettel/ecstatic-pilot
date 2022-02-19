# Some potential parameters

in the QR Code CNNN, for some reason the powersave is set to 2. ncnn::set_cpu_powersave(2); 

description of this:
// bind all threads on little clusters if powersave enabled
// affects HMP arch cpu like ARM big.LITTLE
// only implemented on android at the moment
// switching powersave is expensive and not thread-safe
// 0 = all cores enabled(default)
// 1 = only little clusters enabled
// 2 = only big clusters enabled
// return 0 if success for setter function

I have no idea why this is set to 2. It does not make sense. 

- Chaning the AImagereader resolution did not change performance. 
- Maybe try out setting more threads than cores?
- fiddle with the probability threshhold?
