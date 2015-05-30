
// Spirit2 Tuner Plugin for "NU3001" API:

#define LOGTAG "sftnrbonovo"

#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <sys/ioctl.h>

#include "tnr_tnr.c"

#define AUDIO_CTRL_NODE "/dev/bonovo_handle"
#define HANDLE_CTRL_DEV_MAJOR 230
#define HANDLE_CTRL_DEV_MINOR 0
#define IOCTL_HANDLE_GET_RADIO_STATUS       _IO(HANDLE_CTRL_DEV_MAJOR, 3)
#define IOCTL_HANDLE_START_RADIO_SEARCH     _IO(HANDLE_CTRL_DEV_MAJOR, 4)
#define IOCTL_HANDLE_STOP_RADIO_SEARCH      _IO(HANDLE_CTRL_DEV_MAJOR, 5)
#define IOCTL_HANDLE_GET_RADIO_CURR_FREQ    _IO(HANDLE_CTRL_DEV_MAJOR, 6)
#define IOCTL_HANDLE_GET_RADIO_FREQ         _IO(HANDLE_CTRL_DEV_MAJOR, 7)
#define IOCTL_HANDLE_CODEC_SWITCH_SRC       _IO(HANDLE_CTRL_DEV_MAJOR, 30)
#define IOCTL_HANDLE_CODEC_RECOVER_SRC      _IO(HANDLE_CTRL_DEV_MAJOR, 31)

static int fd_bonovo = -1;

#define RADIO_DEV_NODE "/dev/ttyS3"
#define CMD_RADIO_STANDARD_MODEL    0              
#define CMD_RADIO_BAND_SELECT       1             
#define CMD_RADIO_RDS_ON_OFF        2             
#define CMD_RADIO_VOLUME            3             
#define CMD_RADIO_MUTE              4             
#define CMD_RADIO_FREQ              5             
#define CMD_RADIO_START_SEARCH      6             
#define CMD_RADIO_STOP_SEARCH       7             
#define CMD_RADIO_STEREO_STRENGTHEN 8             
#define CMD_RADIO_SHUTDOWN          9             
#define CMD_RADIO_MIN_FREQ          10            
#define CMD_RADIO_MAX_FREQ          11            
#define CMD_RADIO_STEP_LEN          12            
#define CMD_RADIO_REMOTE            13             
static int fd_radio = -1;

typedef enum
{
	CODEC_LEVEL_NO_ANALOG = 0,
    CODEC_LEVEL_BT_MUSIC = 1,
    CODEC_LEVEL_AV_IN = 2,
    CODEC_LEVEL_DVB = 3,
    CODEC_LEVEL_DVD = 4,
    CODEC_LEVEL_RADIO = 5,
    CODEC_LEVEL_BT_TEL = 6,
    CODEC_LEVEL_COUNT
}CODEC_Level;

static int seek_stop();
static int seek_start();
static int freq_get();

static unsigned int checkSum(unsigned char* cmdBuf, int size) {
  unsigned int sum = 0;
  int i;
  for (i = 0; i < size; i++) {
    sum += cmdBuf[i];
  }
  return sum;
}

static int send_command(const char cmd, const char param1, const char param2) {
  int rv = 0;
  unsigned int sum = 0;
  unsigned char cmdBuf[10] = {0xFA, 0xFA, 0x0A, 0x00,
                              0xA1, cmd,  param1, param2};

  if ((fd_radio < 0) || (fd_bonovo < 0)) {
    logd("The radio was not opened.\n");
    return ENODEV;
  }

  sum = checkSum(cmdBuf, sizeof(cmdBuf) - 2);
  cmdBuf[8] = sum & 0xFF;
  cmdBuf[9] = (sum >> 8) & 0xFF;

  if (rv = write(fd_radio, cmdBuf, cmdBuf[2]) < 0) {
    int err = errno;
    logd("write " RADIO_DEV_NODE " error(%d, %s)\n", err, strerror(err));
  }
  return rv;
}

static int band_set(int low, int high, int band, int step_len) {
  logd("band_set low: %d  high: %d  band: %d", low, high, band);
  int ret = send_command(CMD_RADIO_BAND_SELECT, band, 0);
  if (ret < 0) {
    loge("band_set ret: %d errno: %d (%s)", ret, errno, strerror(errno));
    return (ret);
  }
  logd("band_set success");

  ret = send_command(CMD_RADIO_MIN_FREQ, low & 0x0FF, (low >> 8) & 0x0FF);
  if (ret < 0) {
    loge("set_max_freq ret: %d errno: %d (%s)", ret, errno, strerror(errno));
    return (ret);
  }
  logd("set_min_freq success");

  ret = send_command(CMD_RADIO_MAX_FREQ, high & 0x0FF, (high >> 8) & 0x0FF);
  if (ret < 0) {
    loge("set_max_freq ret: %d errno: %d (%s)", ret, errno, strerror(errno));
    return (ret);
  }
  logd("set_max_freq success");

  ret = send_command(CMD_RADIO_STEP_LEN, step_len & 0x0FF,
                     (step_len >> 8) & 0x0FF);
  if (ret < 0) {
    loge("set_step_len ret: %d errno: %d (%s)", ret, errno, strerror(errno));
    return (ret);
  }
  logd("set_step_len success");

  return (band);
}

static int rbds_set(int band) {
  logd("rbds_set band: %d", band);
  return (band);
}

struct radio_freq
{
	unsigned char freq[2];
	unsigned char is_valid;
};

static int freq_get() {  // freq_sg
	struct radio_freq temp;
  int res;

	do {
    res = ioctl(fd_bonovo, IOCTL_HANDLE_GET_RADIO_FREQ, &temp);
    if (res >= 0) {
      curr_freq_int = 10 * (temp.freq[0] + (temp.freq[1] << 8));
    }
  } while(res >= 0);

  // logd("freq_get, freq:%d\n", curr_freq_int);
  curr_seek_state = 0;
  return (curr_freq_int);
}

static int freq_set(int freq) {
  logd("freq_set, freq:%d\n", freq);
  int freq10 = freq / 10;
  int rv = send_command(CMD_RADIO_FREQ, freq10 & 0x0FF, (freq10 >> 8) & 0x0FF);
  if (rv != 0) {
    loge("Could not set radio frequency.");
    return rv;
  }

  curr_freq_int = freq;
  // rds_init();

  return curr_freq_int;
}

int sls_status_chip_imp_pilot_sg_cnt = 0;

static int activeAudio(CODEC_Level codec_mode) {
  logd("Active audio\n");
  int ret = 0;
  if (fd_bonovo < 0) {
    return -1;
  }
  ret = ioctl(fd_bonovo, IOCTL_HANDLE_CODEC_SWITCH_SRC, codec_mode);
  if (ret) {
    loge("[=== BONOVO ===]%s ioctl is error. ret:%d\n", __FUNCTION__, ret);
  }
  return ret;
}

// int sls_status_chip_imp_event_sg_cnt = 0;

static int seek_stop() {
  logd("seek_stop");

  int rv = send_command(CMD_RADIO_STOP_SEARCH, 0, 0);
  if (rv < 0) {
    return (rv);
  }

  curr_seek_state = 0;
  return (curr_seek_state);
}

static int seek_start(int seek_state) {
  logd("seek_start");

  const int bits = (seek_state == 2) ? 0x02 : 0x03;  // up=1, down=2
  int rv = send_command(CMD_RADIO_START_SEARCH, bits, 0);
  if (rv < 0) {
    loge("seek_start ret: %d errno: %d (%s)", rv, errno, strerror(errno));
    return (rv);
  }

  ioctl(fd_bonovo, IOCTL_HANDLE_START_RADIO_SEARCH);
  curr_seek_state = seek_state;

  return freq_get();
}

// Chip API:

//#define  SUPPORT_RDS
#ifdef SUPPORT_RDS
#include "rds_ssl.c"
#else
int rds_poll(unsigned char* rds_grpd) { return (EVT_GET_NONE); }
#endif

int chip_imp_event_sg(unsigned char* rds_grpd) {  // Polling function called
                                                  // every event_sg_ms
                                                  // milliseconds. Not used
                                                  // remotely but could be in
                                                  // future.
  int ret = 0;
  ret = rds_poll(rds_grpd);
  return (ret);
}

int chip_imp_api_mode_sg(int api_mode) {
  if (api_mode == GET) return (curr_api_mode);
  curr_api_mode = api_mode;
  logd("chip_imp_api_mode_sg curr_api_mode: %d", curr_api_mode);
  return (curr_api_mode);
}

static int recoverAudio(CODEC_Level codec_mode) {
  int ret = 0;

  if (fd_bonovo < 0) {
    return -EINVAL;
  }
  ret = ioctl(fd_bonovo, IOCTL_HANDLE_CODEC_RECOVER_SRC, codec_mode);
  if(ret){
    loge("[=== BONOVO ===]%s ioctl is error. ret:%d\n", __FUNCTION__, ret);
  }
  return ret;
}

static int close_dev() {
  logd("close_dev off fd_radio='%d'", fd_radio);
  if (fd_radio < 0) {
    loge("fd_radio was already off (fd_radio < 0)");
    return -EINVAL;
  }
  recoverAudio(CODEC_LEVEL_RADIO);

  if (send_command(CMD_RADIO_SHUTDOWN, 0, 0) < 0) {
    return -EINVAL;
  }

  close(fd_radio);
  close(fd_bonovo);

  return 0;
}

static int open_dev() {
  logd("open_dev");
  if ((fd_radio = open(RADIO_DEV_NODE, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
    loge("open %s failed,fd=%d(%d,%s)\n", RADIO_DEV_NODE, fd_radio, errno,
         strerror(errno));
    return -ENODEV;
  }
  fd_bonovo = open(AUDIO_CTRL_NODE, O_RDWR | O_NOCTTY | O_NONBLOCK);
  if (-1 == fd_bonovo) {
    logd("Can't open " AUDIO_CTRL_NODE "!\n");
    return -ENODEV;
  }

  if (activeAudio(CODEC_LEVEL_RADIO) != 0) {
    loge("Can't switch analog audio source to radio.\n");
  }

  return 0;
}

int chip_imp_api_state_sg(int state) {
  logd("chip_imp_api_state_sg state: %d", state);
  if (state == GET) return (curr_api_state);

  if (state == 0) {
    curr_api_state = 0;
    return close_dev();
  } else {
    int rv = open_dev();
    if (rv == 0) curr_api_state = 1;
    return (curr_api_state);
  }
}

int chip_imp_mode_sg(int mode) {
  if (mode == GET) return (curr_mode);
  curr_mode = mode;
  logd("chip_imp_mode_sg curr_mode: %d", curr_mode);
  return (curr_mode);
}

int chip_imp_state_sg(int state) {
  if (state == GET) return (curr_state);

  logd("chip_imp_state_sg state: %d", state);
  curr_state = state;
  if (state == 0) {
    chip_imp_mute_sg(1);  // Mute for now
    return 0;
  }

  if (curr_rds_state) {
    // TODO - enable RDS
  } else {
    // TODO - disable RDS
  }

  if (chip_imp_api_state_sg(GET) == 0) {
    curr_state = 0;
    loge("API Not ready");
    return (curr_state);
  }

  chip_imp_vol_sg(65535);

  curr_state = 1;
  logd("chip_imp_state_sg curr_state: %d", curr_state);
  return (curr_state);
}

int chip_imp_antenna_sg(int antenna) {
  if (antenna == GET) return (curr_antenna);
  curr_antenna = antenna;
  logd("chip_imp_antenna_sg curr_antenna: %d", curr_antenna);
  return (curr_antenna);
}

int chip_imp_band_sg(int band) {  // 0:EU, 1:US, 2:UU
  if (band == GET) return (curr_band);

  logd("chip_imp_band_sg band: %d", band);

  curr_band = band;

  curr_freq_lo = 8700;
  curr_freq_hi = 10800;

  if (band == 2)          // If Wide
    curr_freq_lo = 7600;  // 65000;

  curr_freq_inc = 5;
  if (band == 1)  // If US
    curr_freq_inc = 10;

  band_set(curr_freq_lo, curr_freq_hi, band, curr_freq_inc);
  rbds_set(band);

  return (band);
}

int chip_imp_freq_sg(int freq) {
  // 10 KHz resolution; 76 MHz = 7600, 108 MHz = 10800
  if (freq == GET) return (freq_get());
  return freq_set(freq);
}

int chip_imp_vol_sg(int vol) {
  if (vol == GET) return (curr_vol);

  int rv;
  uint8_t vol_reg = vol / 655;  // vol_reg = 0 - 100 from vol = 0 - 65535
  logd("chip_imp_vol_sg %d->%d\n", vol, (int)vol_reg);

  if (vol_reg > 100) vol_reg = 100;
  if (rv = send_command(CMD_RADIO_VOLUME, vol_reg, 0) != 0) {
    loge("Could not set radio volume.");
    return rv;
  }

  curr_vol = vol;
  return (curr_vol);
}

int chip_imp_thresh_sg(int thresh) {
  // TODO - what is this?
  if (thresh == GET) return (curr_thresh);
  curr_thresh = thresh;
  logd("chip_imp_thresh_sg curr_thresh: %d", curr_thresh);
  return (curr_thresh);
}

int chip_imp_mute_sg(int mute) {
  if (mute == GET) return (curr_mute);

  logd("chip_imp_mute_sg %d\n", mute);
  int rv;
  if (rv = send_command(CMD_RADIO_MUTE, (mute ? 3 : 0), 0) != 0) {
    loge("Could not set radio mute.");
    return rv;
  }

  curr_mute = mute;
  logd("chip_imp_mute_sg curr_mute: %d", curr_mute);
  return (curr_mute);
}

int chip_imp_softmute_sg(int softmute) {
  if (softmute == GET) return (curr_softmute);

  int rv;
  if (rv = send_command(CMD_RADIO_MUTE, (softmute ? 3 : 0), 0) != 0) {
    loge("Could not set radio mute.");
    return rv;
  }

  curr_softmute = softmute;
  logd("chip_imp_mute_sg curr_softmute: %d", curr_softmute);
  return (curr_softmute);
}

int chip_imp_stereo_sg(int stereo) {
  if (stereo == GET) return (curr_stereo);
  curr_stereo = stereo;
  logd("chip_imp_stereo_sg curr_stereo: %d", curr_stereo);
  return (curr_stereo);
}

int chip_imp_seek_state_sg(int seek_state) {
  if (seek_state == GET) return (curr_seek_state);
  if (seek_state == 0) return (seek_stop());
  return seek_start(seek_state);
}

int chip_imp_rds_state_sg(int rds_state) {
  if (rds_state == GET) return (curr_rds_state);

  curr_rds_state = rds_state;
  logd("chip_imp_rds_state_sg curr_rds_state: %d", curr_rds_state);
  return (curr_rds_state);
}

int chip_imp_rds_af_state_sg(int rds_af_state) {
  if (rds_af_state == GET) return (curr_rds_af_state);
  curr_rds_af_state = rds_af_state;
  logd("chip_imp_rds_af_state_sg curr_rds_af_state: %d", curr_rds_af_state);
  return (curr_rds_af_state);
}

int chip_imp_rssi_sg(int fake_rssi) {
  return -1;  // TODO ??
}

int chip_imp_pilot_sg(int fake_pilot) {
  return -1;  // TODO ??
}

int chip_imp_rds_pi_sg(int rds_pi) {
  if (rds_pi == GET) return (curr_rds_pi);
  int ret = -1;
  // ret = rds_pi_set (rds_pi);
  curr_rds_pi = rds_pi;
  return (curr_rds_pi);
}
int chip_imp_rds_pt_sg(int rds_pt) {
  if (rds_pt == GET) return (curr_rds_pt);
  int ret = -1;
  // ret = rds_pt_set (rds_pt);
  curr_rds_pt = rds_pt;
  return (curr_rds_pt);
}
char* chip_imp_rds_ps_sg(char* rds_ps) {
  if (rds_ps == GETP) return (curr_rds_ps);
  int ret = -1;
  // ret = rds_ps_set (rds_ps);
  strlcpy(curr_rds_ps, rds_ps, sizeof(curr_rds_ps));
  return (curr_rds_ps);
}
char* chip_imp_rds_rt_sg(char* rds_rt) {
  if (rds_rt == GETP) return (curr_rds_rt);
  int ret = -1;
  // ret = rds_rt_set (rds_rt);
  strlcpy(curr_rds_rt, rds_rt, sizeof(curr_rds_rt));
  return (curr_rds_rt);
}

char* chip_imp_extension_sg(char* reg) {
  if (reg == GETP) return (curr_extension);
  int ret = -1;
  strlcpy(curr_extension, reg, sizeof(curr_extension));
  return (curr_extension);
}

// TODO
#define RDS_CDEV_NAME "bonovo_rds"  // device name
#define DEV_MAJOR 237
#define DEV_MINOR 0

#define RDS_IOCTL_START_DATA _IO(DEV_MAJOR, 0)  // request rds data
#define RDS_IOCTL_STOP_DATA \
  _IO(DEV_MAJOR, 1)  // stop transfer rds data to android
