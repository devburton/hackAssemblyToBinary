// Write an asm script to implement SumN 
// (given n, sum up the numbers from 1 to
// n: 1 + 2 + 3 + ... + n).
// n = R0
@R0
D = M
@n
M = D
// i = 1
@i
M = 1
// sum = 0
@sum
M = 0
// LOOP:
// if i > n goto stop
(LOOP)
@i
D = M
@n
D = D - M
@STOP
D; JGT
// else sum = sum + i; i ++;
@i
D = M
@sum
M = D + M 
@i
M = M + 1
@LOOP
0; JMP
// STOP:
(STOP)
// R1 = sum
@sum
D = M
@R1
M = D
// terminate
(END)
@END
0; JMP
