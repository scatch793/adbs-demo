<template>
  <view class="container">
    <text class="title">aDBS 电压测试器</text>
    <text class="value">当前电压: {{ voltage }} V</text>
    <button class="btn" @click="increaseVoltage">增加电压</button>
  </view>
</template>

<script setup>
import { ref } from 'vue';

// 定义一个响应式变量，初始值为 1.0
const voltage = ref(1.0);

// 定义按钮点击后的行为
const increaseVoltage = () => {
  voltage.value += 0.1;
  // 消除 JavaScript 小数计算的精度误差
  voltage.value = Math.round(voltage.value * 10) / 10; 
};
</script>

<style>
/* 这里是类似 MasterGo 里的排版样式 */
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 150px;
}
.title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}
.value {
  font-size: 40px;
  color: #007aff;
  font-weight: bold;
  margin-bottom: 50px;
}
.btn {
  background-color: #007aff;
  color: white;
  width: 60%;
  border-radius: 10px;
}
</style>