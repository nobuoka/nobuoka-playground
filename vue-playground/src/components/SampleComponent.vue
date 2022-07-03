<template>
  <div class="sample-component">
    <span>{{ time.value }}</span>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

interface Data {
  time: {
    value: number;
    counter: number | undefined;
  };
}

export default defineComponent({
  name: 'SampleComponent',
  props: {
    initialTime: Number,
    countUpIntervalInMills: Number,
  },
  data(): Data {
    return {
      time: {
        value: this.initialTime ?? 0,
        counter: undefined,
      },
    }
  },
  mounted() {
    this.time.counter = setInterval(() => {
      this.time.value++;
    }, this.countUpIntervalInMills ?? 1000)
  },
  unmounted() {
    clearInterval(this.time.counter);
  },
});
</script>

<style scoped>
</style>
