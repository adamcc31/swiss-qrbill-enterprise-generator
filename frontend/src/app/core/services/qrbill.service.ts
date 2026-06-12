import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, QrBillRequest, QrBillResponse } from '../models/qrbill.model';

@Injectable({
  providedIn: 'root'
})
export class QrBillService {
  private apiUrl = 'http://localhost:8081/api/v1/qrbill';

  constructor(private http: HttpClient) {}

  generate(request: QrBillRequest): Observable<ApiResponse<QrBillResponse>> {
    return this.http.post<ApiResponse<QrBillResponse>>(`${this.apiUrl}/generate`, request);
  }

  validate(request: QrBillRequest): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.apiUrl}/validate`, request);
  }

  health(): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(`${this.apiUrl}/health`);
  }

  downloadPdf(request: QrBillRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/download`, request, {
      responseType: 'blob',
      observe: 'response'
    });
  }
}
