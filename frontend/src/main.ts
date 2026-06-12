import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { Component } from '@angular/core';
import { QrBillComponent } from './app/features/qrbill/qrbill.component';
import { errorInterceptor } from './app/core/interceptors/error.interceptor';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [QrBillComponent],
  template: `<app-qrbill></app-qrbill>`
})
export class AppComponent {}

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withInterceptors([errorInterceptor])
    )
  ]
}).catch((err) => console.error(err));
