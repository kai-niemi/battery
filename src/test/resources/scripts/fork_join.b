f1 = fork {
  a = 1+1;
  return a;
};

f2 = fork {
  a = 1+2;
  return a;
};

join [f1,f2];

log.info ("%s", [f1.get()]);
log.info ("%s", [f2.get()]);
