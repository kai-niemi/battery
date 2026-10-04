id = jdbc.queryForObject("select gen_random_uuid()");
log.info ("%s", [id]);