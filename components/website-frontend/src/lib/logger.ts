import chalk, { ChalkInstance } from 'chalk';
import log from 'loglevel';
import prefix from 'loglevel-plugin-prefix';

const colors: { [key: string]: ChalkInstance } = {
  TRACE: chalk.magenta,
  DEBUG: chalk.cyan,
  INFO: chalk.blue,
  WARN: chalk.yellow,
  ERROR: chalk.red
};

log.enableAll();

prefix.reg(log);

prefix.apply(log, {
  format(level, name, timestamp) {
    const coloredTimesatmp = chalk.gray(`[${timestamp}]`);
    const coloredLevel = colors[level.toUpperCase()](level);
    return `${coloredTimesatmp} ${coloredLevel}`;
  }
});

export { log as logger };
